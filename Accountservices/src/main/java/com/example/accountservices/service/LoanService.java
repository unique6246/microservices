package com.example.accountservices.service;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.Repo.EmiPaymentRepository;
import com.example.accountservices.Repo.LoanAccountRepository;
import com.example.accountservices.accountUtils.AccountUtils;
import com.example.accountservices.dto.LoanApplicationDTO;
import com.example.accountservices.entity.Account;
import com.example.accountservices.entity.EmiPayment;
import com.example.accountservices.entity.LoanAccount;
import com.example.accountservices.exception.BusinessRuleException;
import com.example.accountservices.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanAccountRepository loanAccountRepository;
    private final EmiPaymentRepository  emiPaymentRepository;
    private final AccountRepository     accountRepository;

    // Interest rates by loan type (annual %)
    private static final Map<String, BigDecimal> LOAN_RATES = Map.of(
            "PERSONAL",  new BigDecimal("12.00"),
            "HOME",      new BigDecimal("8.50"),
            "AUTO",      new BigDecimal("9.75"),
            "EDUCATION", new BigDecimal("7.50")
    );

    // ── Apply for Loan ───────────────────────────────────────────────────────

    @Transactional
    public LoanAccount applyLoan(LoanApplicationDTO dto) {
        // Check linked savings account exists and is active
        Account savings = accountRepository.findAccountByAccountNumber(dto.getLinkedSavingsAccount());
        if (savings == null) {
            throw new ResourceNotFoundException("Account", "accountNumber", dto.getLinkedSavingsAccount());
        }
        if (!"ACTIVE".equals(savings.getStatus())) {
            throw new BusinessRuleException("Linked savings account is not active");
        }

        // No existing NPA loan allowed
        if (loanAccountRepository.existsByCustomerIdAndStatus(dto.getCustomerId(), "NPA")) {
            throw new BusinessRuleException("Cannot apply for a new loan: existing NPA loan found");
        }

        BigDecimal annualRate = LOAN_RATES.getOrDefault(dto.getLoanType(), new BigDecimal("12.00"));
        BigDecimal emiAmount  = calculateEmi(dto.getPrincipalAmount(), annualRate, dto.getTenureMonths());

        // Create a LOAN-type account entry
        Account loanAccount = accountRepository.save(Account.builder()
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountType("LOAN")
                .balance(dto.getPrincipalAmount())  // outstanding principal
                .customerId(dto.getCustomerId())
                .interestRate(annualRate)
                .nomineeName(dto.getNomineeName())
                .build());

        LocalDate firstEmiDate = LocalDate.now().plusMonths(1);

        LoanAccount loan = loanAccountRepository.save(LoanAccount.builder()
                .accountId(loanAccount.getId())
                .customerId(dto.getCustomerId())
                .loanType(dto.getLoanType())
                .principalAmount(dto.getPrincipalAmount())
                .disbursedAmount(dto.getPrincipalAmount())
                .interestRate(annualRate)
                .tenureMonths(dto.getTenureMonths())
                .emiAmount(emiAmount)
                .firstEmiDate(firstEmiDate)
                .nextEmiDate(firstEmiDate)
                .totalEmis(dto.getTenureMonths())
                .linkedSavingsAcc(dto.getLinkedSavingsAccount())
                .build());

        // Credit disbursed amount to savings account
        savings.setBalance(savings.getBalance().add(dto.getPrincipalAmount()));
        accountRepository.save(savings);

        // Generate EMI schedule
        generateEmiSchedule(loan);

        log.info("Loan approved: loanId={}, customer={}, amount={}, EMI={}",
                loan.getId(), dto.getCustomerId(), dto.getPrincipalAmount(), emiAmount);
        return loan;
    }

    // ── Pay EMI ───────────────────────────────────────────────────────────────

    @Transactional
    public EmiPayment payEmi(Long loanId, BigDecimal paymentAmount) {
        LoanAccount loan = loanAccountRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanAccount", "id", loanId));

        if ("CLOSED".equals(loan.getStatus())) {
            throw new BusinessRuleException("Loan is already closed");
        }

        // Find next pending EMI
        List<EmiPayment> pendingEmis = emiPaymentRepository
                .findAllByLoanAccountIdAndStatus(loanId, "PENDING");
        if (pendingEmis.isEmpty()) {
            throw new BusinessRuleException("No pending EMIs found for this loan");
        }

        EmiPayment nextEmi = pendingEmis.get(0);
        BigDecimal penalty = BigDecimal.ZERO;
        if (LocalDate.now().isAfter(nextEmi.getDueDate())) {
            // 2% penalty on overdue amount
            penalty = nextEmi.getTotalPaid().multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        }

        // Deduct EMI + penalty from savings
        Account savings = accountRepository.findAccountByAccountNumber(loan.getLinkedSavingsAcc());
        BigDecimal totalToPay = nextEmi.getTotalPaid().add(penalty);
        if (savings.getBalance().compareTo(totalToPay) < 0) {
            throw new BusinessRuleException("Insufficient balance to pay EMI of " + totalToPay);
        }
        savings.setBalance(savings.getBalance().subtract(totalToPay));
        accountRepository.save(savings);

        // Update EMI record
        nextEmi.setStatus("PAID");
        nextEmi.setPaidDate(LocalDateTime.now());
        nextEmi.setPenaltyAmount(penalty);
        emiPaymentRepository.save(nextEmi);

        // Update loan
        loan.setEmisPaid(loan.getEmisPaid() + 1);
        loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));
        if (loan.getEmisPaid() >= loan.getTotalEmis()) {
            loan.setStatus("CLOSED");
            log.info("Loan fully repaid: loanId={}", loan.getId());
        } else {
            loan.setStatus("ACTIVE");
        }
        loanAccountRepository.save(loan);

        return nextEmi;
    }

    // ── Get loan schedule ─────────────────────────────────────────────────────

    public List<EmiPayment> getLoanSchedule(Long loanId) {
        return emiPaymentRepository.findAllByLoanAccountIdOrderByEmiNumber(loanId);
    }

    public List<LoanAccount> getCustomerLoans(Long customerId) {
        return loanAccountRepository.findAllByCustomerId(customerId);
    }

    public List<LoanAccount> getOverdueLoans() {
        return loanAccountRepository.findOverdueLoans(LocalDate.now());
    }

    // ── Scheduled overdue check (daily at 00:30) ─────────────────────────────

    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void markOverdueLoans() {
        List<LoanAccount> overdue = loanAccountRepository.findOverdueLoans(LocalDate.now());
        for (LoanAccount loan : overdue) {
            if (!"OVERDUE".equals(loan.getStatus())) {
                loan.setStatus("OVERDUE");
                loanAccountRepository.save(loan);
                // Mark unpaid EMIs as overdue
                emiPaymentRepository.findAllByLoanAccountIdAndStatus(loan.getId(), "PENDING")
                        .stream()
                        .filter(e -> e.getDueDate().isBefore(LocalDate.now()))
                        .forEach(e -> { e.setStatus("OVERDUE"); emiPaymentRepository.save(e); });
                log.warn("Loan marked OVERDUE: loanId={}, customer={}", loan.getId(), loan.getCustomerId());
            }
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Annuity EMI formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     */
    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal r  = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP); // monthly rate
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal pow = onePlusR.pow(months, new MathContext(10));
        BigDecimal emi = principal
                .multiply(r)
                .multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        return emi;
    }

    private void generateEmiSchedule(LoanAccount loan) {
        BigDecimal outstanding = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        LocalDate dueDate = loan.getFirstEmiDate();

        List<EmiPayment> schedule = new ArrayList<>();
        for (int i = 1; i <= loan.getTotalEmis(); i++) {
            BigDecimal interestPart   = outstanding.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart  = loan.getEmiAmount().subtract(interestPart).setScale(2, RoundingMode.HALF_UP);
            outstanding = outstanding.subtract(principalPart).max(BigDecimal.ZERO);

            schedule.add(EmiPayment.builder()
                    .loanAccountId(loan.getId())
                    .emiNumber(i)
                    .interestComponent(interestPart)
                    .principalComponent(principalPart)
                    .totalPaid(loan.getEmiAmount())
                    .dueDate(dueDate)
                    .build());
            dueDate = dueDate.plusMonths(1);
        }
        emiPaymentRepository.saveAll(schedule);
    }
}


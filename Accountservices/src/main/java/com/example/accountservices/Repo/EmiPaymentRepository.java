package com.example.accountservices.Repo;

import com.example.accountservices.entity.EmiPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmiPaymentRepository extends JpaRepository<EmiPayment, Long> {
    List<EmiPayment> findAllByLoanAccountIdOrderByEmiNumber(Long loanAccountId);
    List<EmiPayment> findAllByLoanAccountIdAndStatus(Long loanAccountId, String status);
    int countByLoanAccountIdAndStatus(Long loanAccountId, String status);
}


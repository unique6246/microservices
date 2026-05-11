package com.example.coustomerservices.service;

import com.example.coustomerservices.Repo.KycDocumentRepository;
import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.KycDocumentDTO;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.entity.KycDocument;
import com.example.coustomerservices.exception.BusinessRuleException;
import com.example.coustomerservices.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerRepository    customerRepository;

    // ── Submit KYC document ──────────────────────────────────────────────────

    @Transactional
    public KycDocumentDTO uploadDocument(KycDocumentDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", dto.getCustomerId()));

        if ("VERIFIED".equals(customer.getKycStatus())) {
            throw new BusinessRuleException("Customer KYC is already verified");
        }

        KycDocument doc = kycDocumentRepository.save(KycDocument.builder()
                .customerId(dto.getCustomerId())
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .documentUrl(dto.getDocumentUrl())
                .build());

        // Move customer to UNDER_REVIEW on first document
        if ("PENDING".equals(customer.getKycStatus())) {
            customer.setKycStatus("UNDER_REVIEW");
            customerRepository.save(customer);
        }

        log.info("KYC document uploaded: customerId={}, type={}", dto.getCustomerId(), dto.getDocumentType());
        return toDTO(doc);
    }

    // ── Admin: review document ───────────────────────────────────────────────

    @Transactional
    public KycDocumentDTO reviewDocument(Long docId, String newStatus, String rejectionReason) {
        KycDocument doc = kycDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", "id", docId));

        doc.setStatus(newStatus);
        doc.setReviewedAt(LocalDateTime.now());
        if ("REJECTED".equals(newStatus)) {
            doc.setRejectionReason(rejectionReason);
        }
        kycDocumentRepository.save(doc);

        // Check if all docs are verified → auto-approve KYC
        Customer customer = customerRepository.findById(doc.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", doc.getCustomerId()));
        if (kycDocumentRepository.hasEnoughVerifiedDocs(doc.getCustomerId())) {
            customer.setKycStatus("VERIFIED");
            customer.setKycVerifiedAt(LocalDateTime.now());
            log.info("KYC verified for customerId={}", doc.getCustomerId());
        } else if ("REJECTED".equals(newStatus)) {
            customer.setKycStatus("REJECTED");
        }
        customerRepository.save(customer);

        return toDTO(doc);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public String getKycStatus(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
        return customer.getKycStatus();
    }

    public List<KycDocumentDTO> getDocuments(Long customerId) {
        return kycDocumentRepository.findAllByCustomerId(customerId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private KycDocumentDTO toDTO(KycDocument doc) {
        return KycDocumentDTO.builder()
                .id(doc.getId())
                .customerId(doc.getCustomerId())
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .documentUrl(doc.getDocumentUrl())
                .status(doc.getStatus())
                .rejectionReason(doc.getRejectionReason())
                .build();
    }
}


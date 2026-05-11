package com.example.coustomerservices.Repo;

import com.example.coustomerservices.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findAllByCustomerId(Long customerId);
    long countByCustomerIdAndStatus(Long customerId, String status);

    @Query("SELECT CASE WHEN COUNT(d) >= 2 THEN true ELSE false END FROM KycDocument d " +
           "WHERE d.customerId = :customerId AND d.status = 'VERIFIED'")
    boolean hasEnoughVerifiedDocs(@Param("customerId") Long customerId);
}


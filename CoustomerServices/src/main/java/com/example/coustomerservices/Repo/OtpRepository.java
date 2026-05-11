package com.example.coustomerservices.Repo;

import com.example.coustomerservices.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord> findFirstByCustomerIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long customerId, String purpose, LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpRecord o WHERE o.expiresAt < :now")
    int deleteExpiredOtps(@Param("now") LocalDateTime now);
}


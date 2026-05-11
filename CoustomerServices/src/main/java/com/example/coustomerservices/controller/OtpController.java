package com.example.coustomerservices.controller;

import com.example.coustomerservices.dto.OtpRequestDTO;
import com.example.coustomerservices.dto.OtpVerifyDTO;
import com.example.coustomerservices.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "One-Time Password generation and verification")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/generate")
    @Operation(summary = "Generate an OTP for a given purpose")
    public ResponseEntity<Map<String, String>> generateOtp(@Valid @RequestBody OtpRequestDTO dto) {
        log.info("OTP generate request: customerId={}, purpose={}", dto.getCustomerId(), dto.getPurpose());
        String otp = otpService.generateOtp(dto.getCustomerId(), dto.getPurpose());
        // NOTE: In production, remove 'otp' from the response – it's sent via email
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully to registered email",
                "otp", otp   // Remove in production
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify an OTP")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyDTO dto) {
        log.info("OTP verify request: customerId={}, purpose={}", dto.getCustomerId(), dto.getPurpose());
        boolean valid = otpService.verifyOtp(dto.getCustomerId(), dto.getOtp(), dto.getPurpose());
        return ResponseEntity.ok(Map.of("valid", valid, "message", "OTP verified successfully"));
    }
}


package com.eduai.mailservice.service.mail;

import com.eduai.mailservice.dto.request.OtpRequestDto;
import com.eduai.mailservice.dto.response.OtpResponseDto;

/**
 * OTP email service contract.
 */
public interface OtpService {

    /**
     * Generate and send an OTP email.
     *
     * @param request OTP request
     * @return OTP send response
     */
    OtpResponseDto sendOtp(OtpRequestDto request);
}

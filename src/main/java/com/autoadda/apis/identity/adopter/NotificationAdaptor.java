package com.autoadda.apis.identity.adopter;

import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.exception.EmailSendingException;
import com.autoadda.apis.identity.exception.OtpExpiredException;
import com.autoadda.apis.identity.exception.OtpNotSentException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.request.EmailRequest;
import com.autoadda.apis.identity.request.SmsRequest;
import com.autoadda.apis.identity.request.VerifyOtpRequest;
import com.autoadda.apis.identity.response.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NotificationAdaptor {
    public static final String CONTACTS = "contacts";
    public static final String FAILED_TO_SEND_OTP_STATUS_RESPONSE = "Failed to send OTP. Status: {}, Response: {}";
    public static final String SUCCESS = "success";
    public static final String API_KEY = "api_key";
    public static final String SENDERID = "senderid";
    public static final String SMS_RESPONSE = "SMS response: ";
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;
    @Value("${notification.service.sms.api.url}")
    private String smsApiUrl;
    @Value("${notification.service.sms.api.key}")
    private String apiKey;
    @Value("${notification.service.sms.sender.id}")
    private String smsSenderId;

    private final Map<String, Map.Entry<String, LocalDateTime>> otpStorage = new ConcurrentHashMap<>();

    // Constructor injection for RestTemplate
    public NotificationAdaptor(UserRepository userRepository, RestTemplate restTemplate, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.mailSender = mailSender;
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();

        return String.valueOf(random.nextInt(900000) + 100000);
    }


    @Retryable(
            value = {RestClientException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 10000, multiplier = 2, maxDelay = 15000)
    )
    public GenericResponse callSMSServiceToSendOtp(String mobileNumber) {
        String otp = generateSecureOtp();
        log.info("OTP generated:--------1> " + otp);
        log.info("Attempting SMS send to: {} via {}", mobileNumber, smsApiUrl);
        log.info("otp storage is ---------2> " + otpStorage.get(mobileNumber));
        otpStorage.remove(mobileNumber);
        log.info("otp storage is ---------3> " + otpStorage.get(mobileNumber));
        otpStorage.put(mobileNumber, Map.entry(otp, LocalDateTime.now().plusMinutes(5)));
        log.info("expiry time is 5" + otpStorage.get(mobileNumber));
        otp = String.format("Your OTP is: %s. Do not share this with anyone.", otp);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(API_KEY, apiKey);
        params.add("type", "text");
        params.add(CONTACTS, mobileNumber);
        params.add(SENDERID, smsSenderId);
        params.add("msg", otp);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        log.info("payload: " + entity.toString());
        ResponseEntity<String> response = restTemplate.exchange(
                smsApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        log.info(SMS_RESPONSE + response.getBody());
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("SMS sent successfully to: {}", mobileNumber);
            return GenericResponse.builder()
                    .status(SUCCESS)
                    .code("200")
                    .message("OTP sent successfully")
                    .build();
        } else {
            log.error(FAILED_TO_SEND_OTP_STATUS_RESPONSE,
                    response.getStatusCodeValue(),
                    response.getBody());
            throw new OtpNotSentException("Failed to send OTP. Status: " + response.getStatusCodeValue());
        }

    }

    @Recover
    public GenericResponse recoverFromSmsSendFailure(
            RestClientException e, String mobileNumber) {
        log.error("Failed to send SMS after 3 retry attempts to: {}", mobileNumber, e);
        throw new OtpNotSentException("SMS service temporarily unavailable. Please try again later.");
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        String mobileNumber = request.getMobileNumber();
        String otp = request.getOtp();
        log.info("request ---------5> " + request.getOtp() + "   " + request.getMobileNumber());
        Map.Entry<String, LocalDateTime> entry = otpStorage.get(mobileNumber);
        if (entry == null) {
            return false;
        }
        String storedOtp = entry.getKey();
        log.info("stored otp --------6> " + storedOtp);
        LocalDateTime expiryTime = entry.getValue();
        if (otp.equals(storedOtp)) {
            log.info("otp matched  --------7>");
            if (LocalDateTime.now().isBefore(expiryTime)) {
                otpStorage.remove(mobileNumber);
                return true;
            } else {
                throw new OtpExpiredException("OTP has expired");
            }
        }
        return false;
    }

    @Retryable(
            value = {RestClientException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 10000, multiplier = 2, maxDelay = 15000)
    )
    public GenericResponse sendSms(SmsRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(API_KEY, apiKey);
        params.add("type", "text");
        params.add(CONTACTS, request.getPhoneNumber());
        params.add(SENDERID, smsSenderId);
        params.add("msg", request.getMessage());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                smsApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        log.info(SMS_RESPONSE + response.getBody());
        if (response.getStatusCode().is2xxSuccessful()) {
            return GenericResponse.builder()
                    .status(SUCCESS)
                    .code("200")
                    .message("message  sent successfully")
                    .build();
        } else {
            log.error(FAILED_TO_SEND_OTP_STATUS_RESPONSE,
                    response.getStatusCodeValue(),
                    response.getBody());
            throw new OtpNotSentException("Failed to send OTP. Status: " + response.getStatusCodeValue());
        }
    }

    @Recover
    public GenericResponse recoverFromSmsSendFailure(
            RestClientException e, SmsRequest request) {
        log.error("Failed to send SMS after retries to: {}", request.getPhoneNumber(), e);
        throw new OtpNotSentException("SMS service temporarily unavailable.");
    }

    public GenericResponse sendSms(Integer userId, SmsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String mobileNumber = user.getMobileNumber();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(API_KEY, apiKey);
        params.add("type", "text");
        params.add(CONTACTS, mobileNumber);
        params.add(SENDERID, smsSenderId);
        params.add("msg", request.getMessage());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                smsApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        log.info(SMS_RESPONSE + response.getBody());
        if (response.getStatusCode().is2xxSuccessful()) {
            return GenericResponse.builder()
                    .status(SUCCESS)
                    .code("200")
                    .message("message  sent successfully")
                    .build();
        } else {
            log.error(FAILED_TO_SEND_OTP_STATUS_RESPONSE,
                    response.getStatusCodeValue(),
                    response.getBody());
            throw new OtpNotSentException("Failed to send OTP. Status: " + response.getStatusCodeValue());
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    public GenericResponse sendEmail(Integer userId, EmailRequest request) {
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("user with this id not found"));
        validateUserEmail(user);
        request.setTo(user.getEmail());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", request.getTo());
            return GenericResponse.builder()
                    .status(SUCCESS)
                    .code("200")
                    .message("Email sent successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getTo(), e);
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    @Recover
    public GenericResponse recoverFromEmailSendFailure(Exception e, Integer userId, EmailRequest request) {
        log.error("Failed to send email after retries to user: {}", userId, e);
        throw new EmailSendingException("Email service temporarily unavailable. Please try again later.", e);
    }

    private void validateUserEmail(User user) {
        if(user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
    }

}

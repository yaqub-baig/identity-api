package com.autoadda.apis.identity.adopter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class RecaptchaAdaptor {

    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret}")
    private String secretKey;

    @Value("${recaptcha.verify.url}")
    private String verifyUrl;

    public RecaptchaAdaptor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", secretKey);
        formData.add("response", token);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(verifyUrl, requestEntity, Map.class);

            if (response == null) {
                return false;
            }

            boolean success = Boolean.TRUE.equals(response.get("success"));
            if (!success) {
                log.warn("reCAPTCHA verification failed: {}", response.get("error-codes"));
            }
            return success;
        } catch (RestClientException e) {
            log.error("Failed to call reCAPTCHA verify endpoint", e);
            return false;
        }
    }
}

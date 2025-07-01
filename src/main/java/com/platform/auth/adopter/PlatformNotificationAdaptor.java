package com.platform.auth.adopter;

import com.platform.auth.exception.OtpExpiredException;
import com.platform.auth.exception.OtpNotSentException;
import com.platform.auth.reqest.VerifyOtpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.platform.auth.reqest.NotificationRequest;
import com.platform.auth.response.GenericResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PlatformNotificationAdaptor {
	
	
	@Value("${notification.service.send.email.url}")
	private String notificationServiceUrl;
	@Value("${notification.service.sms.api.url}")
	private String smsApiUrl;
	@Value("${notification.service.sms.api.key}")
	private String apiKey;
	@Value("${notification.service.sms.sender.id}")
	private String senderId ;

	private final Map<String, Map.Entry<String, LocalDateTime>> otpStorage = new ConcurrentHashMap<> ( );

	private String generateSecureOtp() {
		SecureRandom random = new SecureRandom ( );
		return String.valueOf ( random.nextInt ( 900000 ) + 100000 );
	}


	public GenericResponse callNotificationService(NotificationRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<NotificationRequest> httpRequest = new HttpEntity<>(request);
		GenericResponse response = restTemplate.postForObject(notificationServiceUrl, httpRequest, GenericResponse.class);
		return response;
	}

	public GenericResponse callSMSServiceToSendOtp(String mobileNumber) {
		RestTemplate restTemplate = new RestTemplate ( );
		String otp = generateSecureOtp ( );
		log.info ( "Mobile number: " + mobileNumber );
		log.info ( "OTP generated: " + otp );
		log.info("otp storage is ---------> "+otpStorage.get ( mobileNumber ));
		log.info("otp storage is ---------> "+otpStorage.get ( mobileNumber ));
		otpStorage.remove ( mobileNumber );
		log.info("otp storage is ---------> "+otpStorage.get ( mobileNumber ));
		otpStorage.put ( mobileNumber, Map.entry ( otp, LocalDateTime.now ( ).plusMinutes(5) ) );
		log.info("expiry time is "+otpStorage.get ( mobileNumber ));
		otp=String.format("Your OTP is: %s. Do not share this with anyone.", otp);

		try {
			HttpHeaders headers = new HttpHeaders ( );
			headers.setContentType ( MediaType.APPLICATION_FORM_URLENCODED );
			MultiValueMap<String, String> params = new LinkedMultiValueMap<> ( );
			params.add ( "api_key", apiKey );
			params.add ( "type", "text" );
			params.add ( "contacts", mobileNumber );
			params.add ( "senderid", senderId );
			params.add ( "msg", otp );
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<> ( params, headers );

			ResponseEntity<String> response = restTemplate.exchange (
					smsApiUrl,
					HttpMethod.POST,
					entity,
					String.class
			);
			log.info ( "SMS response: " + response.getBody () );
			if (response.getStatusCode().is2xxSuccessful()) {
				return GenericResponse.builder()
						.status("success")
						.code("200")
						.message("OTP sent successfully")
						.build();
			} else {
				log.error("Failed to send OTP. Status: {}, Response: {}",
						response.getStatusCodeValue(),
						response.getBody());
				throw new OtpNotSentException("Failed to send OTP. Status: " + response.getStatusCodeValue());
			}
		}
		catch ( RestClientException e ) {
			log.error("Error while calling SMS service", e);
			throw new OtpNotSentException("Failed to send OTP: " + e.getMessage());
		}

	}

	public boolean verifyOtp(VerifyOtpRequest request) {
		String mobileNumber = request.getMobileNumber ( );
		String otp = request.getOtp ( );
		log.info ( "request ---------> " + request.getOtp ()+"   "+request.getMobileNumber () );
		Map.Entry<String, LocalDateTime> entry = otpStorage.get ( mobileNumber );
		if (entry == null) {
			return false;
		}
		String storedOtp = entry.getKey ( );
		log.info ( "stored otp --------> " + storedOtp );
		LocalDateTime expiryTime = entry.getValue ( );
        if ( otp.equals ( storedOtp ) ) {
			if ( LocalDateTime.now ( ).isBefore ( expiryTime ) ) {
				otpStorage.remove ( mobileNumber );
				return true;
			}
			else {throw new OtpExpiredException ( "OTP has expired" );}
		}
		return false;
	}
}

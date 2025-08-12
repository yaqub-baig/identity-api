package com.autoadda.apis.identity.adopter;

import com.autoadda.apis.identity.exception.OtpExpiredException;
import com.autoadda.apis.identity.exception.OtpNotSentException;
import com.autoadda.apis.identity.request.VerifyOtpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.autoadda.apis.identity.request.NotificationRequest;
import com.autoadda.apis.identity.response.GenericResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NotificationAdaptor {
	
	
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
        return restTemplate.postForObject(notificationServiceUrl, httpRequest, GenericResponse.class);
	}

	public GenericResponse callSMSServiceToSendOtp(String mobileNumber) {
		RestTemplate restTemplate = new RestTemplate ( );
		String otp = generateSecureOtp ( );
		log.info ( "OTP generated:--------1> " + otp );
		log.info ( "OTP generated:--------1> " + mobileNumber );
		log.info("otp storage is ---------2> "+otpStorage.get ( mobileNumber ));
		otpStorage.remove ( mobileNumber );
		log.info("otp storage is ---------3> "+otpStorage.get ( mobileNumber ));
		otpStorage.put ( mobileNumber, Map.entry ( otp, LocalDateTime.now ( ).plusMinutes(5) ) );
		log.info("expiry time is 5"+otpStorage.get ( mobileNumber ));
		otp=String.format("Your OTP is: %s. Do not share this with anyone.", otp);

//		try {
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

	public boolean verifyOtp(VerifyOtpRequest request) {
		String mobileNumber = request.getMobileNumber ( );
		String otp = request.getOtp ( );
		log.info ( "request ---------5> " + request.getOtp ()+"   "+request.getMobileNumber () );
		Map.Entry<String, LocalDateTime> entry = otpStorage.get ( mobileNumber );
		if (entry == null) {
			return false;
		}
		String storedOtp = entry.getKey ( );
		log.info ( "stored otp --------6> " + storedOtp );
		LocalDateTime expiryTime = entry.getValue ( );
        if ( otp.equals ( storedOtp ) ) {
			log.info ( "otp matched  --------7>" );
			if ( LocalDateTime.now ( ).isBefore ( expiryTime ) ) {
				otpStorage.remove ( mobileNumber );
				return true;
			}
			else {throw new OtpExpiredException ( "OTP has expired" );}
		}
		return false;
	}
}

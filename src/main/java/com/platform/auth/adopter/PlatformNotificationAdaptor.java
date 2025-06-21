package com.platform.auth.adopter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.platform.auth.reqest.NotificationRequest;
import com.platform.auth.response.GenericResponse;

@Component
public class PlatformNotificationAdaptor {
	
	
	@Value("${notification.service.send.email.url}")
	private String notificationServiceUrl;
	
	public GenericResponse callNotificationService(NotificationRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<NotificationRequest> httpRequest = new HttpEntity<>(request);
		GenericResponse response = restTemplate.postForObject(notificationServiceUrl, httpRequest, GenericResponse.class);
		return response;
	}

}

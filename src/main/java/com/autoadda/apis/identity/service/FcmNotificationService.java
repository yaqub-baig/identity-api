package com.autoadda.apis.identity.service;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FcmNotificationService {

    public void sendNotification(
            String token,
            String title,
            String body,
            String url) throws FirebaseMessagingException {

        WebpushConfig webpushConfig = WebpushConfig.builder()
            .setNotification(
                WebpushNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putData("url", url)
            .build();

        Message message = Message.builder()
            .setToken(token)
            .setWebpushConfig(webpushConfig)
            .build();

        FirebaseMessaging.getInstance().send(message);
    }
}

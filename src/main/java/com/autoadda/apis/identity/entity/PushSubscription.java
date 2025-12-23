package com.autoadda.apis.identity.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "push_subscription", uniqueConstraints = {
    @UniqueConstraint(name = "unique_user_token_role", columnNames = {"user_id", "fcm_token", "role"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    private String role;

    @Column(name = "fcm_token", length = 500, nullable = false)
    private String fcmToken;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @PrePersist
    protected void onCreate() {
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
    }
}

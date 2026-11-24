package com.autoadda.apis.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** One of a user's saved addresses (home, work, ...), used for deliveries. */
@Entity
@Table(name = "user_address", indexes = @Index(name = "idx_user_address_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /** e.g. Home, Work. */
    @Column(name = "label", length = 50)
    private String label;
    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;
    /** Normalised to +971XXXXXXXXX. */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;
    @Column(name = "address_line2", length = 200)
    private String addressLine2;
    @Column(name = "area", nullable = false, length = 100)
    private String area;
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    @Column(name = "emirate", length = 50)
    private String emirate;
    @Column(name = "map_link", length = 1000)
    private String mapLink;
    private Double latitude;
    private Double longitude;
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

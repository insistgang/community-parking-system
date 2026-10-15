package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_registration_invites", indexes = @Index(
        name = "idx_property_invites_expires_at", columnList = "expires_at"))
public class PropertyRegistrationInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "code_hash", length = 64, nullable = false, unique = true)
    public String codeHash;

    @Column(nullable = false, length = 11)
    public String phone;

    @Column(name = "id_card", nullable = false, length = 18)
    public String idCard;

    @Column(name = "community_name", nullable = false)
    public String communityName;

    @Column(name = "issued_by", nullable = false)
    public Integer issuedBy;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "used_at")
    public LocalDateTime usedAt;
}

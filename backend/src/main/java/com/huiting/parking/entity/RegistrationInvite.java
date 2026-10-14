package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_invites", indexes = @Index(name = "idx_registration_invites_community", columnList = "community_id"))
public class RegistrationInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "code_hash", length = 64, nullable = false, unique = true)
    public String codeHash;

    @Column(name = "community_id", nullable = false)
    public Integer communityId;

    @Column(nullable = false)
    public String phone;

    @Column(name = "id_card", nullable = false)
    public String idCard;

    @Column(name = "issued_by", nullable = false)
    public Integer issuedBy;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "used_at")
    public LocalDateTime usedAt;
}

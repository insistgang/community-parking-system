package com.huiting.parking.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "communities")
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    public String name;
    public String district;
    public String subdistrict;
    @Column(name = "total_spaces")
    public Integer totalSpaces = 50;
    @Column(name = "audit_mode")
    public String auditMode = "THREE_CERT_AUTO";
}

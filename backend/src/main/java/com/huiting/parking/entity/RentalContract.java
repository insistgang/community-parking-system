package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "contract_no", unique = true)
    public String contractNo;
    @Column(name = "space_id")
    public Integer spaceId;
    @Column(name = "user_id")
    public Integer userId;
    @Column(name = "plate_no")
    public String plateNo;
    @Column(name = "start_date")
    public LocalDate startDate;
    @Column(name = "end_date")
    public LocalDate endDate;
    public String status = "ACTIVE";
    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}

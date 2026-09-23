package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spaces")
public class ParkingSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "community_id")
    public Integer communityId;
    @Column(name = "space_no", unique = true)
    public String spaceNo;
    @Column(name = "area_type")
    public String areaType = "GROUND";
    public String status = "VACANT";
    @Column(name = "current_user_id")
    public Integer currentUserId;
    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();
}

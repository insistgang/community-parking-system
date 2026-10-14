package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spaces", uniqueConstraints = @UniqueConstraint(
        name = "uk_parking_spaces_community_no", columnNames = {"community_id", "space_no"}))
public class ParkingSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "community_id")
    public Integer communityId;
    @Column(name = "space_no")
    public String spaceNo;
    @Column(name = "area_type")
    public String areaType = "GROUND";
    public String status = "VACANT";
    @Column(name = "current_user_id")
    public Integer currentUserId;
    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();
}

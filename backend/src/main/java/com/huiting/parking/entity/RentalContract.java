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
    @Column(name = "payment_receipt_no")
    public String paymentReceiptNo;
    @Column(name = "start_date")
    public LocalDate startDate;
    @Column(name = "end_date")
    public LocalDate endDate;
    public String status = "ACTIVE";
    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "legacy_tenant_name")
    public String legacyTenantName;
    @Column(name = "legacy_tenant_phone")
    public String legacyTenantPhone;
    @Column(name = "legacy_id_card")
    public String legacyIdCard;
    @Column(name = "legacy_property_no")
    public String legacyPropertyNo;
    @Column(name = "legacy_start_date_text")
    public String legacyStartDateText;
    @Column(name = "legacy_source_contract_no")
    public String legacySourceContractNo;
    @Column(name = "legacy_car_relationship")
    public String legacyCarRelationship;
    @Column(name = "legacy_import_batch_id", length = 64)
    public String legacyImportBatchId;
    @Column(name = "legacy_source_row")
    public Integer legacySourceRow;
}

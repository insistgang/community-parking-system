package com.huiting.parking.repo;

import com.huiting.parking.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public class Repos {
    public interface DocumentRepo extends JpaRepository<ApplicationDocument, String> {
        List<ApplicationDocument> findByOwnerIdAndApplicationIdIsNullOrderByCreatedAtDesc(Integer ownerId);
        long countByOwnerIdAndApplicationIdIsNull(Integer ownerId);
    }
    public interface ReviewHistoryRepo extends JpaRepository<ApplicationReviewHistory, Integer> {
        List<ApplicationReviewHistory> findByApplicationIdOrderByIdAsc(Integer applicationId);
        boolean existsByApplicationId(Integer applicationId);
    }
    public interface CommunityRepo extends JpaRepository<Community, Integer> {
        Optional<Community> findByName(String name);
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select c from Community c where c.id = :id")
        Optional<Community> lockById(@Param("id") Integer id);
    }
    public interface UserRepo extends JpaRepository<UserAccount, Integer> {
        Optional<UserAccount> findByPhoneOrIdCard(String phone, String idCard);
        Optional<UserAccount> findByPhone(String phone);
        Optional<UserAccount> findByIdCard(String idCard);
        List<UserAccount> findByCommunityIdAndRoleInOrderByIdAsc(Integer communityId, List<String> roles);
        boolean existsByPhoneOrIdCard(String phone, String idCard);
        long countByCommunityIdAndNotesStartingWith(Integer communityId, String prefix);
    }
    public interface SessionRepo extends JpaRepository<AuthSession, Integer> {
        Optional<AuthSession> findByTokenHash(String tokenHash);
        void deleteByTokenHash(String tokenHash);
        void deleteByUserId(Integer userId);
        void deleteByExpiresAtBefore(java.time.LocalDateTime cutoff);
    }
    public interface InviteRepo extends JpaRepository<RegistrationInvite, Integer> {
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select i from RegistrationInvite i where i.codeHash = :codeHash")
        Optional<RegistrationInvite> lockByCodeHash(@Param("codeHash") String codeHash);
        void deleteByExpiresAtBefore(java.time.LocalDateTime cutoff);
    }
    public interface PropertyInviteRepo extends JpaRepository<PropertyRegistrationInvite, Integer> {
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select i from PropertyRegistrationInvite i where i.codeHash = :codeHash")
        Optional<PropertyRegistrationInvite> lockByCodeHash(@Param("codeHash") String codeHash);
        void deleteByExpiresAtBefore(java.time.LocalDateTime cutoff);
    }
    public interface SpaceRepo extends JpaRepository<ParkingSpace, Integer> {
        boolean existsByCommunityIdAndSpaceNo(Integer communityId, String spaceNo);
        List<ParkingSpace> findByCommunityIdOrderBySpaceNoAsc(Integer communityId);
        List<ParkingSpace> findByCommunityIdAndStatusOrderBySpaceNoAsc(Integer communityId, String status);
        long countByCommunityId(Integer communityId);
        long countByCommunityIdAndStatus(Integer communityId, String status);
        long countByCommunityIdAndStatusIn(Integer communityId, List<String> statuses);
    }
    public interface AppRepo extends JpaRepository<QueueApplication, Integer> {
        long countByCommunityIdAndLegacyImportBatchId(Integer communityId, String sourceSha256);
        List<QueueApplication> findByUserIdAndStatusInOrderByIdDesc(Integer userId, List<String> statuses);
        List<QueueApplication> findByCommunityIdAndStatusIn(Integer communityId, List<String> statuses);
        List<QueueApplication> findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(Integer communityId, String status);
        List<QueueApplication> findByCommunityIdOrderByIdDesc(Integer communityId);
        List<QueueApplication> findByCommunityIdAndStatusOrderByIdDesc(Integer communityId, String status);
        List<QueueApplication> findByCommunityIdAndLegacyApplicantPhoneAndUserIdIsNullAndStatus(Integer communityId, String phone, String status);
        Optional<QueueApplication> findFirstByUserIdAndStatusIn(Integer userId, List<String> statuses);
        Optional<QueueApplication> findFirstByUserIdAndStatus(Integer userId, String status);
        Optional<QueueApplication> findFirstByAllocatedSpaceIdAndStatusIn(Integer spaceId, List<String> statuses);
        Optional<QueueApplication> findFirstByAllocatedSpaceId(Integer spaceId);
        List<QueueApplication> findByUserIdAndStatus(Integer userId, String status);
        long countByCommunityIdAndStatus(Integer communityId, String status);
        long countByCommunityIdAndStatusIn(Integer communityId, List<String> statuses);
        List<QueueApplication> findByCommunityIdAndPropertyNoAndStatusIn(Integer communityId, String propertyNo, List<String> statuses);
        List<QueueApplication> findByUserIdOrderByIdDesc(Integer userId);
        List<QueueApplication> findByCommunityIdAndStatusInOrderByAllocatedTimeAsc(Integer communityId, List<String> statuses);
    }
    public interface ContractRepo extends JpaRepository<RentalContract, Integer> {
        long countByLegacyImportBatchId(String sourceSha256);
        Optional<RentalContract> findFirstByUserIdAndStatusIn(Integer userId, List<String> statuses);
        Optional<RentalContract> findFirstBySpaceIdAndStatusNot(Integer spaceId, String status);
        Optional<RentalContract> findFirstBySpaceIdAndStatusIn(Integer spaceId, List<String> statuses);
        List<RentalContract> findBySpaceIdOrderByIdDesc(Integer spaceId);
        List<RentalContract> findBySpaceIdAndStatusNot(Integer spaceId, String status);
        long countByStatus(String status);
    }
    public interface EvidenceRepo extends JpaRepository<ChainEvidence, Integer> {
        Optional<ChainEvidence> findFirstByCommunityIdAndEventTypeAndRawPayloadJsonContainingOrderByIdDesc(Integer communityId, String eventType, String sourceSha256);
        Optional<ChainEvidence> findFirstByOrderByIdDesc();
        Optional<ChainEvidence> findFirstByCommunityIdOrderByIdDesc(Integer communityId);
        Optional<ChainEvidence> findFirstByCommunityIdAndIdLessThanOrderByIdDesc(Integer communityId, Integer id);
        List<ChainEvidence> findByCommunityIdOrderByIdAsc(Integer communityId);
        Optional<ChainEvidence> findByEvidenceId(String evidenceId);
        List<ChainEvidence> findByCommunityIdOrderByBlockHeightDesc(Integer communityId);
        List<ChainEvidence> findByCommunityIdAndUserIdOrCommunityIdAndEventTypeOrderByBlockHeightDesc(Integer communityId, Integer userId, Integer communityId2, String eventType);
        long countByCommunityIdAndEventType(Integer communityId, String eventType);
        List<ChainEvidence> findTop10ByCommunityIdAndRawPayloadJsonContainingOrderByIdDesc(Integer communityId, String spaceNo);
    }
    public interface ImportBatchRepo extends JpaRepository<ImportBatch, Integer> {
        Optional<ImportBatch> findBySourceSha256(String sourceSha256);
    }
    public interface NoticeRepo extends JpaRepository<Notification, Integer> {
        long countByUserIdAndReadFlag(Integer userId, Boolean readFlag);
        List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
        List<Notification> findByReadFlagFalseAndCreatedAtBefore(java.time.LocalDateTime time);
        List<Notification> findByReadFlagFalseAndSmsUpgradedFalseAndCreatedAtBefore(java.time.LocalDateTime time);
    }
    public interface LoginLogRepo extends JpaRepository<LoginLog, Integer> {
        List<LoginLog> findAllByOrderByLoginAtDesc();
        List<LoginLog> findTop50ByOrderByLoginAtDesc();
        List<LoginLog> findByRoleOrderByLoginAtDesc(String role);
        long countByStatus(String status);
    }
    public interface OperationLogRepo extends JpaRepository<OperationLog, Integer> {
        List<OperationLog> findAllByOrderByCreatedAtDesc();
        List<OperationLog> findTop50ByOrderByCreatedAtDesc();
        List<OperationLog> findByModuleOrderByCreatedAtDesc(String module);
    }
    public interface ExternalApiLogRepo extends JpaRepository<ExternalApiLog, Integer> {
        List<ExternalApiLog> findAllByOrderByCreatedAtDesc();
        List<ExternalApiLog> findTop50ByOrderByCreatedAtDesc();
        List<ExternalApiLog> findByApiNameOrderByCreatedAtDesc(String apiName);
        long countByStatus(String status);
    }
}

package com.huiting.parking.repo;

import com.huiting.parking.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class Repos {
    public interface CommunityRepo extends JpaRepository<Community, Integer> {
        Optional<Community> findByName(String name);
    }
    public interface UserRepo extends JpaRepository<UserAccount, Integer> {
        Optional<UserAccount> findByPhoneOrIdCard(String phone, String idCard);
        Optional<UserAccount> findByPhone(String phone);
        Optional<UserAccount> findByIdCard(String idCard);
        List<UserAccount> findByCommunityIdAndRoleInOrderByIdAsc(Integer communityId, List<String> roles);
        boolean existsByPhoneOrIdCard(String phone, String idCard);
    }
    public interface SpaceRepo extends JpaRepository<ParkingSpace, Integer> {
        List<ParkingSpace> findByCommunityIdOrderBySpaceNoAsc(Integer communityId);
        List<ParkingSpace> findByCommunityIdAndStatusOrderBySpaceNoAsc(Integer communityId, String status);
        long countByCommunityId(Integer communityId);
        long countByCommunityIdAndStatus(Integer communityId, String status);
        long countByCommunityIdAndStatusIn(Integer communityId, List<String> statuses);
    }
    public interface AppRepo extends JpaRepository<QueueApplication, Integer> {
        List<QueueApplication> findByUserIdAndStatusInOrderByIdDesc(Integer userId, List<String> statuses);
        List<QueueApplication> findByCommunityIdAndStatusIn(Integer communityId, List<String> statuses);
        List<QueueApplication> findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(Integer communityId, String status);
        List<QueueApplication> findByCommunityIdOrderByIdDesc(Integer communityId);
        List<QueueApplication> findByCommunityIdAndStatusOrderByIdDesc(Integer communityId, String status);
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
        Optional<RentalContract> findFirstByUserIdAndStatusIn(Integer userId, List<String> statuses);
        Optional<RentalContract> findFirstBySpaceIdAndStatusNot(Integer spaceId, String status);
        Optional<RentalContract> findFirstBySpaceIdAndStatusIn(Integer spaceId, List<String> statuses);
        List<RentalContract> findBySpaceIdOrderByIdDesc(Integer spaceId);
        List<RentalContract> findBySpaceIdAndStatusNot(Integer spaceId, String status);
        long countByStatus(String status);
    }
    public interface EvidenceRepo extends JpaRepository<ChainEvidence, Integer> {
        Optional<ChainEvidence> findFirstByOrderByIdDesc();
        Optional<ChainEvidence> findByEvidenceId(String evidenceId);
        List<ChainEvidence> findByCommunityIdOrderByBlockHeightDesc(Integer communityId);
        List<ChainEvidence> findByCommunityIdAndUserIdOrCommunityIdAndEventTypeOrderByBlockHeightDesc(Integer communityId, Integer userId, Integer communityId2, String eventType);
        long countByCommunityIdAndEventType(Integer communityId, String eventType);
        List<ChainEvidence> findTop10ByCommunityIdAndRawPayloadJsonContainingOrderByIdDesc(Integer communityId, String spaceNo);
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

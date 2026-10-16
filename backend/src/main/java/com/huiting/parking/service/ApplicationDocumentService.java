package com.huiting.parking.service;

import com.huiting.parking.common.ApiException;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.ApplicationDocument;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ApplicationDocumentService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDocumentService.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "ID_CARD", "身份证明", "PROPERTY_CERT", "房屋证明",
            "VEHICLE_LICENSE", "车辆行驶证", "RELATIONSHIP_PROOF", "车辆关系证明");
    private final Repos.DocumentRepo documents;
    private final Repos.AppRepo applications;
    private final Repos.CommunityRepo communities;
    private final Path storage;

    public ApplicationDocumentService(Repos.DocumentRepo documents, Repos.AppRepo applications,
            Repos.CommunityRepo communities,
            @Value("${app.documents.storage-path:${user.dir}/data/application-documents}") String storagePath) {
        this.documents = documents;
        this.applications = applications;
        this.communities = communities;
        this.storage = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> drafts(UserAccount user) {
        requireOwner(user);
        String id = user.idCard == null ? "" : user.idCard;
        String masked = id.length() >= 10 ? id.substring(0, 6) + "********" + id.substring(id.length() - 4) : "**";
        return ParkingService.map("applicant_name", user.realName, "phone", user.phone, "id_card_masked", masked,
                "documents", documents.findByOwnerIdAndApplicationIdIsNullOrderByCreatedAtDesc(user.id).stream().map(this::describe).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(UserAccount user, String type, MultipartFile file) throws IOException {
        requireOwner(user);
        if (user.idCard == null || !user.idCard.matches("[0-9]{17}[0-9Xx]"))
            throw new ApiException(403, "身份资料待物业核实，完成后再上传申请材料");
        if (!TYPES.containsKey(type)) throw new ApiException(400, "请选择有效的材料类型");
        if (file.isEmpty()) throw new ApiException(400, "请选择需要上传的文件");
        if (file.getSize() > MAX_BYTES) throw new ApiException(413, "每份材料不能超过10MB");
        byte[] bytes = file.getBytes();
        String mediaType = detectType(bytes);
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("材料").replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        String extension = "application/pdf".equals(mediaType) ? ".pdf" : "image/png".equals(mediaType) ? ".png" : ".jpg";
        if (original.isBlank()) original = TYPES.get(type) + extension;
        if (original.length() > 180) original = original.substring(0, 170) + extension;
        String lower = original.toLowerCase(Locale.ROOT);
        boolean extensionMatches = "application/pdf".equals(mediaType) ? lower.endsWith(".pdf")
                : "image/png".equals(mediaType) ? lower.endsWith(".png") : lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        if (!extensionMatches) throw new ApiException(400, "文件内容与扩展名不符，请上传JPG、PNG或PDF文件");
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        if (applications.findFirstByUserIdAndStatusIn(user.id,
                List.of("PENDING_REVIEW", "WAITING", "ALLOCATED", "OFFLINE_PENDING")).isPresent())
            throw new ApiException(409, "已有办理中的车位申请，请先查看当前进度");
        if (documents.countByOwnerIdAndApplicationIdIsNull(user.id) >= 12)
            throw new ApiException(400, "未提交材料已达到12份，请先移除不需要的草稿材料");
        ApplicationDocument doc = new ApplicationDocument();
        doc.id = UUID.randomUUID().toString();
        doc.ownerId = user.id;
        doc.communityId = user.communityId;
        doc.documentType = type;
        doc.originalName = original;
        doc.contentType = mediaType;
        doc.sizeBytes = (long) bytes.length;
        doc.createdAt = Times.utcNow();
        try { doc.sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        Files.createDirectories(storage);
        if (Files.getFileStore(storage).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(storage, PosixFilePermissions.fromString("rwx------"));
        Path path = filePath(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteFile(path);
            }
        });
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
        if (Files.getFileStore(path).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        documents.save(doc);
        return describe(doc);
    }

    @Transactional
    public void deleteDraft(UserAccount user, String id) {
        requireOwner(user);
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        ApplicationDocument doc = documents.findById(id)
                .filter(d -> d.ownerId.equals(user.id) && d.communityId.equals(user.communityId))
                .orElseThrow(() -> new ApiException(404, "材料不存在"));
        if (doc.applicationId != null) throw new ApiException(409, "材料已随申请提交，不能从历史申请中移除");
        documents.delete(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteFile(filePath(doc)); }
        });
    }

    @Transactional(readOnly = true)
    public ApplicationDocument readable(UserAccount user, String id) {
        ApplicationDocument doc = documents.findById(id).filter(d ->
                ("OWNER".equals(user.role) && d.ownerId.equals(user.id))
                        || ("PROPERTY_ADMIN".equals(user.role) && d.communityId.equals(user.communityId) && d.applicationId != null))
                .orElseThrow(() -> new ApiException(404, "材料不存在或无权查看"));
        if (!Files.isRegularFile(filePath(doc))) throw new ApiException(404, "材料文件不存在，请联系物业核查");
        return doc;
    }

    public List<ApplicationDocument> requireDocuments(UserAccount user, Object rawIds, String relationship) {
        if (!(rawIds instanceof List<?> ids) || ids.isEmpty() || ids.size() > 4)
            throw new ApiException(400, "请上传身份证明、房屋证明和车辆行驶证");
        Set<String> seen = new HashSet<>();
        Set<String> types = new HashSet<>();
        List<ApplicationDocument> selected = new ArrayList<>();
        for (Object value : ids) {
            String id = String.valueOf(value);
            if (!seen.add(id)) throw new ApiException(400, "请勿重复选择同一份材料");
            ApplicationDocument doc = documents.findById(id)
                    .filter(d -> d.ownerId.equals(user.id) && d.communityId.equals(user.communityId) && d.applicationId == null)
                    .orElseThrow(() -> new ApiException(400, "材料无效、已提交或不属于当前申请人，请重新上传"));
            if (!types.add(doc.documentType)) throw new ApiException(400, "同一类材料请选择一份文件，多页材料可合并为PDF");
            if (!Files.isRegularFile(filePath(doc))) throw new ApiException(400, "材料文件缺失，请重新上传");
            selected.add(doc);
        }
        if (!types.containsAll(Set.of("ID_CARD", "PROPERTY_CERT", "VEHICLE_LICENSE")))
            throw new ApiException(400, "身份证明、房屋证明和车辆行驶证均为必填材料");
        if (!"SELF".equals(relationship) && !types.contains("RELATIONSHIP_PROOF"))
            throw new ApiException(400, "配偶或同住家人的车辆还需上传关系证明");
        return selected;
    }

    public void link(List<ApplicationDocument> selected, Integer applicationId) {
        selected.forEach(d -> d.applicationId = applicationId);
        documents.saveAll(selected);
    }

    public Map<String, Object> describe(ApplicationDocument doc) {
        return ParkingService.map("id", doc.id, "name", doc.originalName, "type", doc.documentType,
                "type_label", TYPES.get(doc.documentType), "size_bytes", doc.sizeBytes,
                "content_type", doc.contentType, "sha256", doc.sha256, "uploaded_at", Times.fmtBj(doc.createdAt),
                "verification_status", "PENDING_MANUAL");
    }

    public Path filePath(ApplicationDocument doc) {
        if (!doc.id.matches("[0-9a-f-]{36}")) throw new ApiException(404, "材料不存在");
        return storage.resolve(doc.id + ".bin");
    }

    private void requireOwner(UserAccount user) {
        if (!"OWNER".equals(user.role)) throw new ApiException(403, "仅业主账号可以上传申请材料");
    }

    private static String detectType(byte[] bytes) {
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') return "application/pdf";
        throw new ApiException(400, "仅支持JPG、PNG图片或PDF文件");
    }

    private void deleteFile(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { LOG.error("清理草稿材料文件失败: {}", path.getFileName(), e); }
    }
}

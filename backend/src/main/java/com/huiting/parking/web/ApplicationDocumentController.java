package com.huiting.parking.web;

import com.huiting.parking.common.ApiResponse;
import com.huiting.parking.service.ApplicationDocumentService;
import com.huiting.parking.service.ParkingService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/parking/application-documents")
public class ApplicationDocumentController {
    private final ApplicationDocumentService documents;
    private final ParkingService parking;

    public ApplicationDocumentController(ApplicationDocumentService documents, ParkingService parking) {
        this.documents = documents;
        this.parking = parking;
    }

    @GetMapping
    public ApiResponse drafts(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(documents.drafts(parking.requireUser(uid)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse upload(@RequestAttribute(name = "authUserId") Integer uid,
            @RequestParam("document_type") String type, @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok("材料已保存，提交申请后由物业人工核验", documents.upload(parking.requireUser(uid), type, file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileSystemResource> download(@RequestAttribute(name = "authUserId") Integer uid, @PathVariable String id) {
        var doc = documents.readable(parking.requireUser(uid), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(doc.contentType))
                .contentLength(doc.sizeBytes)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.originalName, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(documents.filePath(doc)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@RequestAttribute(name = "authUserId") Integer uid, @PathVariable String id) {
        documents.deleteDraft(parking.requireUser(uid), id);
        return ApiResponse.msg("草稿材料已移除");
    }
}

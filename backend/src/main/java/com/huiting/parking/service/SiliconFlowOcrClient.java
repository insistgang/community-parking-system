package com.huiting.parking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Optional OCR adapter. Production traffic remains disabled until the sample and privacy review are complete. */
@Service
public class SiliconFlowOcrClient {
    private static final Set<String> TYPES = Set.of("ID_CARD", "PROPERTY_CERT", "VEHICLE_LICENSE", "RELATIONSHIP_PROOF");
    private final boolean enabled;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper json;

    @Autowired
    public SiliconFlowOcrClient(
            @Value("${app.ocr.siliconflow.enabled:false}") boolean enabled,
            @Value("${app.ocr.siliconflow.api-key:${SILICONFLOW_API_KEY:}}") String apiKey,
            @Value("${app.ocr.siliconflow.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${app.ocr.siliconflow.model:Qwen/Qwen3-VL-8B-Instruct}") String model,
            ObjectMapper json) {
        this(enabled, apiKey, baseUrl, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), json);
    }

    public SiliconFlowOcrClient(boolean enabled, String apiKey, String baseUrl, String model, HttpClient http, ObjectMapper json) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        this.model = model;
        this.http = http;
        this.json = json;
    }

    public record Result(String status, String model, JsonNode fields) {}

    public Result extract(String documentType, String contentType, byte[] imageBytes) {
        if (!TYPES.contains(documentType)) return new Result("UNSUPPORTED_DOCUMENT", model, null);
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType))
            return new Result("MANUAL_REVIEW_REQUIRED", model, null);
        if (!enabled) return new Result("DISABLED", model, null);
        if (apiKey.isBlank()) return new Result("NOT_CONFIGURED", model, null);
        if (imageBytes == null || imageBytes.length == 0 || imageBytes.length > 10 * 1024 * 1024)
            return new Result("MANUAL_REVIEW_REQUIRED", model, null);
        try {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
            String prompt = switch (documentType) {
                case "ID_CARD" -> "读取身份证明可见信息，仅输出JSON对象：name,id_number。看不清的字段用null，不推断身份真伪。";
                case "PROPERTY_CERT" -> "读取房屋证明可见信息，仅输出JSON对象：rights_holder_names（数组）,property_address,certificate_number。看不清的字段用null，不判断申请资格。";
                case "VEHICLE_LICENSE" -> "读取行驶证可见信息，仅输出JSON对象：owner_name,plate_number。看不清的字段用null，不判断申请资格。";
                default -> "读取关系证明可见信息，仅输出JSON对象：names（数组）,relationship_text。看不清的字段用null，不判断申请资格。";
            };
            String requestJson = json.writeValueAsString(Map.of(
                    "model", model,
                    "stream", false,
                    "temperature", 0,
                    "max_tokens", 500,
                    "messages", List.of(Map.of("role", "user", "content", List.of(
                            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl, "detail", "high")),
                            Map.of("type", "text", "text", prompt))))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) return new Result("RATE_LIMITED", model, null);
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                return new Result("TECHNICAL_FAILURE", model, null);
            JsonNode content = json.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) return new Result("UNREADABLE", model, null);
            JsonNode fields = json.readTree(content.asText());
            if (!fields.isObject()) return new Result("UNREADABLE", model, null);
            Set<String> allowed = switch (documentType) {
                case "ID_CARD" -> Set.of("name", "id_number");
                case "PROPERTY_CERT" -> Set.of("rights_holder_names", "property_address", "certificate_number");
                case "VEHICLE_LICENSE" -> Set.of("owner_name", "plate_number");
                default -> Set.of("names", "relationship_text");
            };
            ObjectNode extracted = json.createObjectNode();
            boolean anyReadable = false;
            for (String key : allowed) {
                JsonNode value = fields.path(key);
                if (value.isTextual()) {
                    String text = value.asText().trim();
                    if (text.isEmpty()) extracted.putNull(key);
                    else { extracted.put(key, text); anyReadable = true; }
                } else if (value.isArray() && value.size() > 0 && java.util.stream.StreamSupport.stream(value.spliterator(), false).allMatch(JsonNode::isTextual)) {
                    extracted.set(key, value);
                    anyReadable = true;
                } else extracted.putNull(key);
            }
            return anyReadable ? new Result("EXTRACTED_UNVERIFIED", model, extracted) : new Result("UNREADABLE", model, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result("TECHNICAL_FAILURE", model, null);
        } catch (Exception e) {
            // Never log image bytes, credentials, or recognized identity fields.
            return new Result("TECHNICAL_FAILURE", model, null);
        }
    }
}

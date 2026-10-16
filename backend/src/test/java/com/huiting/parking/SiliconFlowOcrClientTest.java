package com.huiting.parking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.service.SiliconFlowOcrClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SiliconFlowOcrClientTest {
    @Test
    void usesVisionContractAndTreatsExtractionAsUnverified() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"{\\\"name\\\":\\\"测试用户\\\",\\\"id_number\\\":null}\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ObjectMapper json = new ObjectMapper();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            SiliconFlowOcrClient client = new SiliconFlowOcrClient(true, "test-key", baseUrl,
                    "Qwen/Qwen3-VL-8B-Instruct", java.net.http.HttpClient.newHttpClient(), json);
            var result = client.extract("ID_CARD", "image/png", new byte[]{(byte) 137, 80, 78, 71});
            assertThat(result.status()).isEqualTo("EXTRACTED_UNVERIFIED");
            assertThat(result.fields().path("name").asText()).isEqualTo("测试用户");
            assertThat(result.fields().path("id_number").isNull()).isTrue();
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(requestBody.get()).contains("Qwen/Qwen3-VL-8B-Instruct", "data:image/png;base64,");
            assertThat(client.extract("PROPERTY_CERT", "application/pdf", new byte[]{1}).status()).isEqualTo("MANUAL_REVIEW_REQUIRED");
            assertThat(new SiliconFlowOcrClient(false, "", baseUrl, "Qwen/Qwen3-VL-8B-Instruct",
                    java.net.http.HttpClient.newHttpClient(), json)
                    .extract("ID_CARD", "image/png", new byte[]{1}).status()).isEqualTo("DISABLED");
        } finally {
            server.stop(0);
        }
    }
}

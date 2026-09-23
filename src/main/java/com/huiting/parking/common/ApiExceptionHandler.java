package com.huiting.parking.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handle(ApiException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.status);
        body.put("message", e.getMessage());
        body.put("detail", e.getMessage());
        body.put("data", null);
        return ResponseEntity.status(e.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 500);
        body.put("message", "服务器内部异常: " + e.getMessage());
        body.put("detail", e.getMessage());
        body.put("data", null);
        return ResponseEntity.status(500).body(body);
    }
}

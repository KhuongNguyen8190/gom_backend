package com.gom.badminton.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PingController {

    /**
     * API Endpoint phục vụ mục đích chống ngủ đông (Keep-Alive) cho Render.com
     * Tuyệt đối không gọi xuống Repository/Database để tiết kiệm tài nguyên Aiven.
     * * @return Trạng thái hệ thống kèm mốc thời gian phản hồi thực tế
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> pingServer() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "GOM Badminton API đang vận hành ổn định.");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
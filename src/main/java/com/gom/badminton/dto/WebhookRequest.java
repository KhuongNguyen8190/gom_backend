package com.gom.badminton.dto;

import java.math.BigDecimal;

public class WebhookRequest {
    private String content;          // Nội dung chuyển khoản (Ví dụ: "GOM T3S3049")
    private BigDecimal transferAmount; // Số tiền thực nhận

    // Getters và Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
}
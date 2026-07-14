package com.gom.badminton.dto;

import java.math.BigDecimal;

public class WebhookRequest {
    private String content;
    private BigDecimal transferAmount;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
}
package com.multilingual.chat.app.dto;

public class ReadReceiptDto {

    private final String type = "READ_RECEIPT";
    private Long readerId;   // who read the messages
    private Long senderId;   // whose messages were read

    public ReadReceiptDto() {}

    public ReadReceiptDto(Long readerId, Long senderId) {
        this.readerId = readerId;
        this.senderId = senderId;
    }

    public String getType()    { return type; }
    public Long getReaderId()  { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public Long getSenderId()  { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
}

package com.secureexam.desktop;

public class AuditLog {
    private String userId;
    private String action;
    private String examName;
    private String enteredCode;
    private long timestamp;

    public AuditLog(String userId, String action, String examName, String enteredCode, long timestamp) {
        this.userId = userId;
        this.action = action;
        this.examName = examName;
        this.enteredCode = enteredCode;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public String getEnteredCode() { return enteredCode; }
    public void setEnteredCode(String enteredCode) { this.enteredCode = enteredCode; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
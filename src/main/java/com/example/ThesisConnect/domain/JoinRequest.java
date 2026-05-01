package com.example.ThesisConnect.domain;

public class JoinRequest {

    private Long requestId;

    private User sender;

    private User recipient;

    private ThesisGroup group;

    private RequestStatus status;

    private RequestType requestType;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public ThesisGroup getGroup() {
        return group;
    }

    public void setGroup(ThesisGroup group) {
        this.group = group;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
}

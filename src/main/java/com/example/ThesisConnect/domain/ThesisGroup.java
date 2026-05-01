package com.example.ThesisConnect.domain;

import java.util.ArrayList;
import java.util.List;

public class ThesisGroup {

    private Long groupId;

    private String topic;

    private String description;

    private User admin;

    private List<User> members = new ArrayList<>();

    private List<String> documents = new ArrayList<>();

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents == null ? new ArrayList<>() : new ArrayList<>(documents);
    }
}

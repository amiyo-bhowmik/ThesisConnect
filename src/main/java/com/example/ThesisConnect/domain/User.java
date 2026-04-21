package com.example.ThesisConnect.domain;

import java.util.ArrayList;
import java.util.List;

public class User {

    private Long userId;

    private String name;

    private String email;

    private String password;

    private String department;

    private String university;

    private String academicDetails;

    private String bio;

    private String profilePicture;

    private List<String> researchInterests = new ArrayList<>();

    private List<String> skills = new ArrayList<>();

    private boolean isLookingForGroup;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getAcademicDetails() {
        return academicDetails;
    }

    public void setAcademicDetails(String academicDetails) {
        this.academicDetails = academicDetails;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public List<String> getResearchInterests() {
        return researchInterests;
    }

    public void setResearchInterests(List<String> researchInterests) {
        this.researchInterests = researchInterests == null
                ? new ArrayList<>()
                : new ArrayList<>(researchInterests);
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills == null
                ? new ArrayList<>()
                : new ArrayList<>(skills);
    }

    public boolean isLookingForGroup() {
        return isLookingForGroup;
    }

    public void setLookingForGroup(boolean lookingForGroup) {
        isLookingForGroup = lookingForGroup;
    }
}

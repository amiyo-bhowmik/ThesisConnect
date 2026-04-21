package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    private String email;

    @Size(max = 120)
    private String department;

    @Size(max = 150)
    private String university;

    @Size(max = 500)
    private String academicDetails;

    @Size(max = 800)
    private String bio;

    private List<@Size(max = 80) String> researchInterests;

    private List<@Size(max = 80) String> skills;

    private boolean lookingForGroup;

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

    public List<String> getResearchInterests() {
        return researchInterests;
    }

    public void setResearchInterests(List<String> researchInterests) {
        this.researchInterests = researchInterests;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public boolean isLookingForGroup() {
        return lookingForGroup;
    }

    public void setLookingForGroup(boolean lookingForGroup) {
        this.lookingForGroup = lookingForGroup;
    }
}

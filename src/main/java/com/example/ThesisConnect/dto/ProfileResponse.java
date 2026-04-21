package com.example.ThesisConnect.dto;

import java.util.List;

public record ProfileResponse(
        Long userId,
        String name,
        String email,
        String department,
        String university,
        String academicDetails,
        String bio,
        String profilePicture,
        List<String> researchInterests,
        List<String> skills,
        boolean lookingForGroup
) {
}

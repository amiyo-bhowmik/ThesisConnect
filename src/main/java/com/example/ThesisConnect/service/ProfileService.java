package com.example.ThesisConnect.service;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ProfileUpdateRequest;

public class ProfileService {

    public ProfileResponse getProfile(String email) {
        return mapToResponse(findByEmail(email));
    }

    public ProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = findByEmail(email);
        if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setDepartment(trimToNull(request.getDepartment()));
        user.setUniversity(trimToNull(request.getUniversity()));
        user.setAcademicDetails(trimToNull(request.getAcademicDetails()));
        user.setBio(trimToNull(request.getBio()));
        user.setResearchInterests(cleanList(request.getResearchInterests()));
        user.setSkills(cleanList(request.getSkills()));
        user.setLookingForGroup(request.isLookingForGroup());

        return mapToResponse(userRepository.save(user));
    }

    private ProfileResponse mapToResponse(User user) {
        return new ProfileResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getUniversity(),
                user.getAcademicDetails(),
                user.getBio(),
                user.getProfilePicture(),
                List.copyOf(user.getResearchInterests()),
                List.copyOf(user.getSkills()),
                user.isLookingForGroup()
        );
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
                .map(this::trimToNull)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    

}

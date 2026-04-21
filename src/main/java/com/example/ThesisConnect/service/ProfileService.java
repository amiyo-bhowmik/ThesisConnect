package com.example.ThesisConnect.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
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
    
    public ProfileResponse uploadProfilePicture(String email, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select an image");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, or WEBP images are allowed");
        }

        User user = findByEmail(email);
        try {
            Files.createDirectories(uploadRoot);
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String safeExtension = extension == null ? "png" : extension.toLowerCase();
            String fileName = UUID.randomUUID() + "." + safeExtension;
            Path destination = uploadRoot.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePicture("/uploads/" + fileName);
            return mapToResponse(userRepository.save(user));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store profile picture");
        }
    }

    public List<ProfileResponse> searchStudents(
            String email,
            String name,
            String studentEmail,
            String researchInterest,
            String department,
            String university,
            Boolean lookingForGroupOnly
    ) {
        User currentUser = findByEmail(email);
        return userRepository.searchStudents(
                        currentUser.getUserId(),
                        trimToNull(name),
                        trimToNull(studentEmail),
                        trimToNull(researchInterest),
                        trimToNull(department),
                        trimToNull(university),
                        lookingForGroupOnly
                ).stream()
                .map(this::mapToResponse)
                .toList();
    }


}

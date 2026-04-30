package com.example.ThesisConnect.service;

import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ProfileUpdateRequest;
import com.example.ThesisConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final Path uploadRoot;

    public ProfileService(UserRepository userRepository, @Value("${app.upload.dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public ProfileResponse getProfile(String email) {
        return mapToResponse(findByEmail(email));
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

    public ProfileResponse getStudentProfile(String email, Long userId) {
        findByEmail(email);
        return userRepository.findById(userId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
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

    public void deleteProfile(String email) {
        User user = findByEmail(email);
        deleteStoredProfilePicture(user.getProfilePicture());
        userRepository.deleteById(user.getUserId());
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void deleteStoredProfilePicture(String profilePicture) {
        if (profilePicture == null || !profilePicture.startsWith("/uploads/")) {
            return;
        }

        Path picturePath = uploadRoot.resolve(profilePicture.substring("/uploads/".length())).normalize();
        if (!picturePath.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(picturePath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete profile picture");
        }
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

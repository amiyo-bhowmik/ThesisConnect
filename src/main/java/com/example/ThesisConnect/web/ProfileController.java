package com.example.ThesisConnect.web;

import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ProfileUpdateRequest;
import com.example.ThesisConnect.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @GetMapping("/students")
    public List<ProfileResponse> searchStudents(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String interest,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String university,
            @RequestParam(required = false) Boolean lookingForGroup
    ) {
        return profileService.searchStudents(
                authentication.getName(),
                name,
                email,
                interest,
                department,
                university,
                lookingForGroup
        );
    }

    @GetMapping("/students/{userId}")
    public ProfileResponse viewStudentProfile(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return profileService.getStudentProfile(authentication.getName(), userId);
    }

    @PutMapping("/me")
    public ProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        ProfileResponse response = profileService.updateProfile(authentication.getName(), request);
        if (!authentication.getName().equalsIgnoreCase(response.email())) {
            UsernamePasswordAuthenticationToken updatedAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            response.email(),
                            authentication.getCredentials(),
                            authentication.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
        }
        return response;
    }

    @PostMapping(path = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return profileService.uploadProfilePicture(authentication.getName(), file);
    }

    @DeleteMapping("/me")
    public void deleteProfile(Authentication authentication) {
        profileService.deleteProfile(authentication.getName());
    }
}

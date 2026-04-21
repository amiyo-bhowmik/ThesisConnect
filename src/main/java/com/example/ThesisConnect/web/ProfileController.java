package com.example.ThesisConnect.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ProfileUpdateRequest;

import jakarta.validation.Valid;

public class ProfileController {
    @GetMapping("/me")
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
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

}

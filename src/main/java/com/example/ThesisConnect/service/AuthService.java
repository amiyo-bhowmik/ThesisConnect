package com.example.ThesisConnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.ThesisConnect.dto.AuthResponse;
import com.example.ThesisConnect.dto.RegisterRequest;

public class AuthService {
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResearchInterests(List.of());
        user.setSkills(List.of());
        user.setLookingForGroup(false);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }

        return new AuthResponse(jwtService.generateToken(savedUser.getEmail()), mapToProfile(savedUser));
    }
    
}

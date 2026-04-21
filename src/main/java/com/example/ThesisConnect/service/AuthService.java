package com.example.ThesisConnect.service;

import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.dto.AuthResponse;
import com.example.ThesisConnect.dto.LoginRequest;
import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.RegisterRequest;
import com.example.ThesisConnect.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

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

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account Not Found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong Password");
        }

        return new AuthResponse(jwtService.generateToken(user.getEmail()), mapToProfile(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private ProfileResponse mapToProfile(User user) {
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
}

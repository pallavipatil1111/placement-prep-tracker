package com.placementprep.service;

import com.placementprep.domain.User;
import com.placementprep.repository.UserRepository;
import com.placementprep.security.JwtService;
import com.placementprep.web.dto.AuthResponse;
import com.placementprep.web.dto.LoginRequest;
import com.placementprep.web.dto.RegisterRequest;
import com.placementprep.web.dto.UserView;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalise(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public UserView me(Long userId) {
        return UserView.of(load(userId));
    }

    @Transactional
    public UserView setDailyGoal(Long userId, int dailyGoal) {
        if (dailyGoal < 1 || dailyGoal > 50) {
            throw ApiException.badRequest("Daily goal must be between 1 and 50");
        }
        User user = load(userId);
        user.setDailyGoal(dailyGoal);
        return UserView.of(users.save(user));
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(jwtService.issue(user), jwtService.ttlSeconds(), UserView.of(user));
    }

    static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package com.placementprep.web;

import com.placementprep.security.AuthUser;
import com.placementprep.service.AuthService;
import com.placementprep.web.dto.AuthResponse;
import com.placementprep.web.dto.GoalRequest;
import com.placementprep.web.dto.LoginRequest;
import com.placementprep.web.dto.RegisterRequest;
import com.placementprep.web.dto.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/api/me")
    public UserView me(@AuthenticationPrincipal AuthUser me) {
        return authService.me(me.id());
    }

    @PutMapping("/api/me/goal")
    public UserView setGoal(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody GoalRequest request) {
        return authService.setDailyGoal(me.id(), request.dailyGoal());
    }
}

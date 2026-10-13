package com.placementprep.service;

import static com.placementprep.service.TestFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.placementprep.domain.User;
import com.placementprep.repository.UserRepository;
import com.placementprep.security.JwtService;
import com.placementprep.web.dto.LoginRequest;
import com.placementprep.web.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository users;
    private PasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        JwtService jwt = new JwtService("test-secret-test-secret-test-secret-123", 60, CLOCK);
        service = new AuthService(users, encoder, jwt);
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static HttpStatus status(Throwable e) {
        return ((ApiException) e).getStatus();
    }

    @Test
    void duplicateEmailIsAConflictRegardlessOfCase() {
        when(users.existsByEmailIgnoreCase("pallavi@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("P", "  Pallavi@Example.COM ", "password1")))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).save(any());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        User u = TestFixtures.user(1L);
        when(users.findByEmailIgnoreCase("pallavi@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("pallavi@example.com", "wrong")));
        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("nobody@example.com", "whatever")));

        assertThat(status(wrongPassword)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(unknown)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getMessage()).isEqualTo(unknown.getMessage());
    }

    @Test
    void dailyGoalIsValidatedAndSaved() {
        User u = TestFixtures.user(1L);
        when(users.findById(1L)).thenReturn(Optional.of(u));
        assertThat(service.setDailyGoal(1L, 5).dailyGoal()).isEqualTo(5);
        assertThatThrownBy(() -> service.setDailyGoal(1L, 0)).satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.setDailyGoal(1L, 51)).isInstanceOf(ApiException.class);
    }

    @Test
    void emailsAreNormalised() {
        assertThat(AuthService.normalise("  Demo@Prep.IN ")).isEqualTo("demo@prep.in");
    }

    @Test
    void shortJwtSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 60, CLOCK)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}

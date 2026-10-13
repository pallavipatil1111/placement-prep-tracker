package com.placementprep.security;

/** The authenticated caller, resolved from the JWT and injected with {@code @AuthenticationPrincipal}. */
public record AuthUser(Long id, String email) {
}

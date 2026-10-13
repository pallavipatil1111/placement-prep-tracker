package com.placementprep.service;

import com.placementprep.domain.User;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Shared test data: a fixed "today" in IST and a saved user. */
final class TestFixtures {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    static final Clock CLOCK = Clock.fixed(ZonedDateTime.of(TODAY.atTime(10, 0), IST).toInstant(), IST);

    private TestFixtures() {
    }

    static User user(long id) {
        User u = new User("Pallavi", "pallavi@example.com", "hash");
        u.setId(id);
        return u;
    }
}

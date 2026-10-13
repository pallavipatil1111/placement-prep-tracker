package com.placementprep.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Today" depends on the user's time zone: a problem solved at 11 pm IST must count for that
 * day's streak, not the next UTC day. Everything date-related reads this clock, which also lets
 * tests pin the date.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.zone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}

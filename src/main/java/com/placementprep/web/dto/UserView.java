package com.placementprep.web.dto;

import com.placementprep.domain.User;

public record UserView(Long id, String name, String email, int dailyGoal) {

    public static UserView of(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getDailyGoal());
    }
}

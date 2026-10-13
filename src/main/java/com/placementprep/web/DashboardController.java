package com.placementprep.web;

import com.placementprep.security.AuthUser;
import com.placementprep.service.DashboardService;
import com.placementprep.web.dto.DashboardView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/dashboard")
    public DashboardView dashboard(@AuthenticationPrincipal AuthUser me,
                                   @RequestParam(defaultValue = "" + DashboardService.DEFAULT_WEEKS) int weeks) {
        return service.build(me.id(), weeks);
    }
}

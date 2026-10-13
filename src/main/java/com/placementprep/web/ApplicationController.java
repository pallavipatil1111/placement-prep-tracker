package com.placementprep.web;

import com.placementprep.core.Stage;
import com.placementprep.security.AuthUser;
import com.placementprep.service.ApplicationService;
import com.placementprep.web.dto.ApplicationRequest;
import com.placementprep.web.dto.ApplicationView;
import com.placementprep.web.dto.StageChangeView;
import com.placementprep.web.dto.StageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationView> list(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(required = false) Stage stage) {
        return service.list(me.id(), stage);
    }

    @GetMapping("/{id}")
    public ApplicationView get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.get(me.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ApplicationRequest request) {
        return service.create(me.id(), request);
    }

    @PutMapping("/{id}")
    public ApplicationView update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody ApplicationRequest request) {
        return service.update(me.id(), id, request);
    }

    @PatchMapping("/{id}/stage")
    public ApplicationView move(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @Valid @RequestBody StageRequest request) {
        return service.move(me.id(), id, request.stage());
    }

    @GetMapping("/{id}/history")
    public List<StageChangeView> history(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.history(me.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me.id(), id);
    }
}

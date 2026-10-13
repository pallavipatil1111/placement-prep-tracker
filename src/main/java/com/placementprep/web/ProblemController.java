package com.placementprep.web;

import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Topic;
import com.placementprep.security.AuthUser;
import com.placementprep.service.ProblemService;
import com.placementprep.web.dto.ProblemRequest;
import com.placementprep.web.dto.ProblemView;
import com.placementprep.web.dto.ReviewRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProblemView> list(@AuthenticationPrincipal AuthUser me,
                                  @RequestParam(required = false) Topic topic,
                                  @RequestParam(required = false) Difficulty difficulty,
                                  @RequestParam(required = false) String q) {
        return service.list(me.id(), topic, difficulty, q);
    }

    @GetMapping("/due")
    public List<ProblemView> due(@AuthenticationPrincipal AuthUser me) {
        return service.due(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemView create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProblemRequest request) {
        return service.create(me.id(), request);
    }

    @PostMapping("/{id}/review")
    public ProblemView review(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                              @Valid @RequestBody ReviewRequest request) {
        return service.review(me.id(), id, request.quality());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me.id(), id);
    }
}

package com.example.leaderboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {
    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @PostMapping(value = "/{username}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LeaderboardService.ScoreUpdate updateScore(
            @PathVariable("username") @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
            String username,
            @Valid @RequestBody ScoreRequest request) {
        return service.updateScore(username, request.score());
    }

    @GetMapping("/top/{n}")
    public List<LeaderboardService.RankedPlayer> top(
            @PathVariable("n") @Min(1) @Max(1000) int count) {
        return service.top(count);
    }

    public record ScoreRequest(
            @NotNull @Min(0) @Max(9_007_199_254_740_991L) Long score) {}
}

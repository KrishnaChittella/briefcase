package com.krishna.briefcase.leaderboard;

import com.krishna.briefcase.leaderboard.dto.AttorneyRankingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping
    public List<AttorneyRankingResponse> leaderboard(
            @RequestParam(defaultValue = "REVENUE") RankingMetric metric,
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriod period) {
        return service.getLeaderboard(metric, period);
    }
}

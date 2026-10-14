package com.krishna.briefcase.leaderboard;

import com.krishna.briefcase.leaderboard.dto.AttorneyRankingResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    private final RankingStrategyFactory factory;

    public LeaderboardService(RankingStrategyFactory factory) {
        this.factory = factory;
    }

    public List<AttorneyRankingResponse> getLeaderboard(RankingMetric metric, LeaderboardPeriod period) {
        List<AttorneyRanking> rankings = factory.get(metric).rank(period.startInstant());
        List<AttorneyRankingResponse> result = new ArrayList<>(rankings.size());
        int rank = 1;
        for (AttorneyRanking ranking : rankings) {
            result.add(new AttorneyRankingResponse(rank++, ranking.attorneyId(), ranking.attorneyName(), ranking.score()));
        }
        return result;
    }
}

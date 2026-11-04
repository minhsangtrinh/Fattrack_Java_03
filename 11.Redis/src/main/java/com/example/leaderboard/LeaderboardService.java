package com.example.leaderboard;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {
    private final StringRedisTemplate primary;
    private final StringRedisTemplate replica;
    private final String key;

    public LeaderboardService(
            @Qualifier("primaryRedisTemplate") StringRedisTemplate primary,
            @Qualifier("replicaRedisTemplate") StringRedisTemplate replica,
            @Value("${leaderboard.redis.key}") String key) {
        this.primary = primary;
        this.replica = replica;
        this.key = key;
    }

    public ScoreUpdate updateScore(String username, long score) {
        // ZADD: create the member or replace its score, rather than incrementing it.
        Boolean added = primary.opsForZSet().add(key, username, score);
        if (added == null) {
            throw new DataAccessResourceFailureException("Missing Redis write result");
        }
        return new ScoreUpdate(username, score);
    }

    public List<RankedPlayer> top(int count) {
        // Spring Data maps this call to ZREVRANGE key 0 (count - 1) WITHSCORES.
        var tuples = replica.opsForZSet().reverseRangeWithScores(key, 0, count - 1L);
        if (tuples == null) {
            throw new DataAccessResourceFailureException("Missing Redis read result");
        }
        List<RankedPlayer> players = new ArrayList<>(tuples.size());
        long rank = 1;
        for (var tuple : tuples) {
            String username = tuple.getValue();
            Double score = tuple.getScore();
            if (username == null || score == null) {
                throw new DataAccessResourceFailureException("Invalid leaderboard entry");
            }
            players.add(new RankedPlayer(rank++, username, score.longValue()));
        }
        return players;
    }

    public record ScoreUpdate(String username, long score) {}

    public record RankedPlayer(long rank, String username, long score) {}
}

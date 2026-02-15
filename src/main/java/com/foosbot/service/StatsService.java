package com.foosbot.service;

import com.foosbot.repository.StatsRepository;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class StatsService {

    private final StatsRepository statsRepository;

    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public Map<String, List<Map<String, Object>>> getWeeklyStats() {
        return getStats(false);
    }

    public Map<String, List<Map<String, Object>>> getAllTimeStats() {
        return getStats(true);
    }

    public void resetAll() {
        statsRepository.resetAll();
    }

    private Map<String, List<Map<String, Object>>> getStats(boolean allTime) {
        Map<String, List<Map<String, Object>>> stats = new LinkedHashMap<>();
        stats.put("Player Stats", statsRepository.getPlayerStats(allTime));
        stats.put("Forward Stats", statsRepository.getForwardStats(allTime));
        stats.put("Goalie Stats", statsRepository.getGoalieStats(allTime));
        return stats;
    }
}

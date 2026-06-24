package com.petgame.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StartupInitializer {
    private final JdbcTemplate jdbcTemplate;

    public StartupInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("create table if not exists game_state (" +
                "id integer primary key check (id = 1)," +
                "state_json text not null," +
                "updated_at text not null" +
                ")");
        Integer count = jdbcTemplate.queryForObject("select count(*) from game_state", Integer.class);
        if (count == null || count == 0) {
            String bootstrap = "{\"playerName\":\"训练师小艾\",\"money\":1200,\"gems\":24,\"day\":1,\"autoTick\":0,\"statusMessage\":\"开店准备中。\",\"pets\":[],\"inventory\":[],\"history\":[]}";
            jdbcTemplate.update("insert into game_state (id, state_json, updated_at) values (1, ?, datetime('now'))", bootstrap);
        }
    }
}

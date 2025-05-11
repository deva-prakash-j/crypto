package com.crypto.repository.impl;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.crypto.entity.OpenInterest;
import com.crypto.repository.OpenInterestCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OpenInterestCustomRepositoryImpl implements OpenInterestCustomRepository{

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO open_interest (symbol, interval, market_type, open_interest, open_interest_value, timestamp) 
        VALUES (:symbol, :interval, :marketType, :open_interest, :open_interest_value, :timestamp)
        ON CONFLICT (symbol, market_type, timestamp, interval) DO NOTHING
    """;

    @Override
    public void bulkInsertIgnoreConflicts(List<OpenInterest> dataList) {
       if (dataList.isEmpty()) return;

        SqlParameterSource[] batchParams = dataList.stream()
        .map(data -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("symbol", data.getSymbol());
            params.addValue("marketType", data.getMarketType().name()); // 👈 convert enum to String
            params.addValue("interval", data.getInterval());
            params.addValue("open_interest", data.getOpenInterest());
            params.addValue("open_interest_value", data.getOpenInterestValue());
            params.addValue("timestamp", data.getTimestamp());
            return params;
        })
        .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(SQL, batchParams);
    }
}

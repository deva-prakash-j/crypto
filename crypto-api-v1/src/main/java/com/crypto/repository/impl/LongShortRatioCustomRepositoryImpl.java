package com.crypto.repository.impl;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.crypto.entity.LiquidationData;
import com.crypto.entity.LongShortRatio;
import com.crypto.repository.LongShortRatioCustomRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class LongShortRatioCustomRepositoryImpl implements LongShortRatioCustomRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO long_short_ratio (symbol, interval, type, long_account_ratio, short_account_ratio, long_short_ratio, timestamp) 
        VALUES (:symbol, :interval, :type, :long_account_ratio, :short_account_ratio, :long_short_ratio, :timestamp)
        ON CONFLICT (symbol, type, timestamp, interval) DO NOTHING
    """;

    @Override
    public void bulkInsertIgnoreConflicts(List<LongShortRatio> dataList) {
       if (dataList.isEmpty()) return;

        SqlParameterSource[] batchParams = dataList.stream()
        .map(data -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("symbol", data.getSymbol());
            params.addValue("type", data.getType()); 
            params.addValue("interval", data.getInterval());
            params.addValue("long_account_ratio", data.getLongAccountRatio());
            params.addValue("short_account_ratio", data.getShortAccountRatio());
            params.addValue("long_short_ratio", data.getLongShortRatio());
            params.addValue("timestamp", data.getTimestamp());
            return params;
        })
        .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(SQL, batchParams);
    }
}

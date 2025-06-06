package com.crypto.repository.impl;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.crypto.entity.LiquidationData;
import com.crypto.repository.LiquidationDataCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LiquidationDataCustomRepositoryImpl implements LiquidationDataCustomRepository {
    
     private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO liquidation_data (symbol, interval, market_type, liquidation_long, liquidation_short, timestamp) 
        VALUES (:symbol, :interval, :marketType, :liquidation_long, :liquidation_short, :timestamp)
        ON CONFLICT (symbol, market_type, timestamp, interval) DO NOTHING
    """;

    @Override
    public void bulkInsertIgnoreConflicts(List<LiquidationData> dataList) {
       if (dataList.isEmpty()) return;

        SqlParameterSource[] batchParams = dataList.stream()
        .map(data -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("symbol", data.getSymbol());
            params.addValue("marketType", data.getMarketType().name()); // 👈 convert enum to String
            params.addValue("interval", data.getInterval());
            params.addValue("liquidation_long", data.getLiquidationLong());
            params.addValue("liquidation_short", data.getLiquidationShort());
            params.addValue("timestamp", data.getTimestamp());
            return params;
        })
        .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(SQL, batchParams);
    }

}

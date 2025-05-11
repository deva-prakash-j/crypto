package com.crypto.repository.impl;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.crypto.entity.FundingRate;
import com.crypto.repository.FundingRateCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FundingRatecustomRepositoryImpl implements FundingRateCustomRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO funding_rate (symbol, market_type, funding_time, funding_rate, mark_price) 
        VALUES (:symbol, :marketType, :funding_time, :funding_rate,:mark_price)
        ON CONFLICT (symbol, market_type, funding_time) DO NOTHING
    """;

    @Override
    public void bulkInsertIgnoreConflicts(List<FundingRate> dataList) {
       if (dataList.isEmpty()) return;

        SqlParameterSource[] batchParams = dataList.stream()
        .map(data -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("symbol", data.getSymbol());
            params.addValue("marketType", data.getMarketType().name()); // 👈 convert enum to String
            params.addValue("funding_time", data.getFundingTime());
            params.addValue("funding_rate", data.getFundingRate());
            params.addValue("mark_price", data.getFundingTime());
            return params;
        })
        .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(SQL, batchParams);
    }
    
}

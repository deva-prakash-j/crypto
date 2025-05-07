package com.crypto.repository.impl;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.crypto.entity.OhlcvData;
import com.crypto.repository.OhlcvDataRepositoryCustom;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OhlcvDataRepositoryImpl implements OhlcvDataRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO ohlcv_data (
            symbol, market_type, interval, open_time,
            open, high, low, close,
            volume, quote_volume, close_time,
            trade_count, taker_buy_volume, taker_buy_quote_volume
        ) VALUES (
            :symbol, :marketType, :interval, :openTime,
            :open, :high, :low, :close,
            :volume, :quoteVolume, :closeTime,
            :tradeCount, :takerBuyVolume, :takerBuyQuoteVolume
        )
        ON CONFLICT (symbol, market_type, interval, open_time) DO NOTHING
    """;

    @Override
    @Transactional
    public void bulkInsertIgnoreConflicts(List<OhlcvData> dataList) {
        if (dataList.isEmpty()) return;

        SqlParameterSource[] batchParams = dataList.stream()
        .map(data -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("symbol", data.getSymbol());
            params.addValue("interval", data.getInterval());
            params.addValue("marketType", data.getMarketType().name()); // 👈 convert enum to String
            params.addValue("openTime", data.getOpenTime());
            params.addValue("open", data.getOpen());
            params.addValue("high", data.getHigh());
            params.addValue("low", data.getLow());
            params.addValue("close", data.getClose());
            params.addValue("volume", data.getVolume());
            params.addValue("quoteVolume", data.getQuoteVolume());
            params.addValue("closeTime", data.getCloseTime());
            params.addValue("tradeCount", data.getTradeCount());
            params.addValue("takerBuyVolume", data.getTakerBuyVolume());
            params.addValue("takerBuyQuoteVolume", data.getTakerBuyQuoteVolume());
            return params;
        })
        .toArray(SqlParameterSource[]::new);


        jdbc.batchUpdate(SQL, batchParams);
    }
}

package com.crypto.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import com.crypto.entity.OhlcvData;

@Service
public class OhlcvDataService {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	public void bulkInsertIgnoreConflicts(List<OhlcvData> entities) {
	    String sql = """
	        INSERT INTO ohlcv_data (
			  timestamp, pair, interval,
			  open, high, low, close,
			  first_trade_timestamp, last_trade_timestamp,
			  first_trade_price, last_trade_price,
			  high_trade_price, high_trade_timestamp,
			  low_trade_price, low_trade_timestamp,
			  volume, quote_volume,
			  volume_buy, quote_volume_buy,
			  volume_sell, quote_volume_sell,
			  volume_unknown, quote_volume_unknown,
			  total_trades, total_buy_trades, total_sell_trades, total_unknown_trades,
			  last_updated
			) VALUES (
			  :timestamp, :pair, :interval,
			  :open, :high, :low, :close,
			  :firstTradeTimestamp, :lastTradeTimestamp,
			  :firstTradePrice, :lastTradePrice,
			  :highTradePrice, :highTradeTimestamp,
			  :lowTradePrice, :lowTradeTimestamp,
			  :volume, :quoteVolume,
			  :volumeBuy, :quoteVolumeBuy,
			  :volumeSell, :quoteVolumeSell,
			  :volumeUnknown, :quoteVolumeUnknown,
			  :totalTrades, :totalBuyTrades, :totalSellTrades, :totalUnknownTrades,
			  :lastUpdated
			)
			ON CONFLICT (pair, interval, timestamp) DO NOTHING;
	    """;

	    SqlParameterSource[] batchParams = entities.stream()
	        .map(BeanPropertySqlParameterSource::new)
	        .toArray(SqlParameterSource[]::new);

	    jdbcTemplate.batchUpdate(sql, batchParams);
	}
}

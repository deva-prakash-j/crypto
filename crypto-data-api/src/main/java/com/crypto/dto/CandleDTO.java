package com.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CandleDTO {
	
	@JsonProperty("UNIT")
	private String unit;
	
	@JsonProperty("TIMESTAMP")
	private long timestamp;
	
	@JsonProperty("TYPE")
	private String type;
	
	@JsonProperty("MARKET")
	private String market;
	
	@JsonProperty("INSTRUMENT")
	private String instrument;
	
	@JsonProperty("MAPPED_INSTRUMENT")
	private String mappedInstrument;
	
	@JsonProperty("BASE")
	private String base;
	
	@JsonProperty("QUOTE")
	private String quote;
	
	@JsonProperty("BASE_ID")
	private long baseId;
	
	@JsonProperty("QUOTE_ID")
	private long quoteId;
	
	@JsonProperty("TRANSFORM_FUNCTION")
	private String transformFunction;
	
	@JsonProperty("OPEN")
	private double open;
	
	@JsonProperty("HIGH")
	private double high;
	
	@JsonProperty("LOW")
	private double low;
	
	@JsonProperty("CLOSE")
	private double close;
	
	@JsonProperty("FIRST_TRADE_TIMESTAMP")
	private long firstTradeTimestamp;
	
	@JsonProperty("LAST_TRADE_TIMESTAMP")
	private long lastTradeTimestamp;
	
	@JsonProperty("FIRST_TRADE_PRICE")
	private double firstTradePrice;
	
	@JsonProperty("HIGH_TRADE_PRICE")
	private double highTradePrice;
	
	@JsonProperty("HIGH_TRADE_TIMESTAMP")
	private long highTradeTimestamp;
	
	@JsonProperty("LOW_TRADE_PRICE")
	private double lowTradePrice;
	
	@JsonProperty("LOW_TRADE_TIMESTAMP")
	private long lowTradeTimestamp;
	
	@JsonProperty("LAST_TRADE_PRICE")
	private double lastTradePrice;
	
	@JsonProperty("VOLUME")
	private double volume;
	
	@JsonProperty("QUOTE_VOLUME")
	private double quoteVolume;
	
	@JsonProperty("VOLUME_BUY")
	private double volumeBuy;
	
	@JsonProperty("QUOTE_VOLUME_BUY")
	private double quoteVolumeBuy;
	
	@JsonProperty("VOLUME_SELL")
	private double volumeSell;
	
	@JsonProperty("QUOTE_VOLUME_SELL")
	private double quoteVolumeSell;
	
	@JsonProperty("VOLUME_UNKNOWN")
	private double volumeUnknown;
	
	@JsonProperty("QUOTE_VOLUME_UNKNOWN")
	private double quoteVolumeUnknown;
	
	@JsonProperty("TOTAL_TRADES")
	private long totalTrades;
	
	@JsonProperty("TOTAL_TRADES_BUY")
	private long totalTradesBuy;
	
	@JsonProperty("TOTAL_TRADES_SELL")
	private long totalTradesSell;
	
	@JsonProperty("TOTAL_TRADES_UNKNOWN")
	private long totalTradesUnknown;
	
}


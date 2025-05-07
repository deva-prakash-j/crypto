package com.crypto.clients.response;

import java.util.List;

import lombok.Data;

@Data
public class BinanceSymbolInfo {
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private String status;
    private Integer pricePrecision;
    private Integer quantityPrecision;
    private List<Object> filters; // Placeholder – not needed now
    private String contractType;     // Only present in FUTURES
    private String marginAsset;      // Only present in FUTURES
}
package com.crypto.service;

import com.crypto.entity.MarketType;

public interface AggTradeService {
    public void syncAggTradeData(String symbol, MarketType marketType);
}

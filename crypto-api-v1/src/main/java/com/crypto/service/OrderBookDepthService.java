package com.crypto.service;

import com.crypto.entity.MarketType;

public interface OrderBookDepthService {
    
    public void syncBookDepth(String symbol, MarketType type);
}

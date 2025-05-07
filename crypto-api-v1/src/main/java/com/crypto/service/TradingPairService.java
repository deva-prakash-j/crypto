package com.crypto.service;

import java.util.List;

import com.crypto.dto.TradingPairDTO;
import com.crypto.entity.MarketType;

public interface TradingPairService {
    void syncSpotTradingPairs();
    void syncFuturesTradingPairs();
    List<TradingPairDTO> getTradingPairsByMarketType(MarketType marketType);
    List<TradingPairDTO> getAllActiveTradingPairs();

}

package com.crypto.service;

import java.util.List;

import com.crypto.dto.CoinalyzeMarketInfoDTO;
import com.crypto.entity.MarketType;

public interface CoinalyzeService {
    
    public List<CoinalyzeMarketInfoDTO> fetchMarketData(MarketType marketType);
}

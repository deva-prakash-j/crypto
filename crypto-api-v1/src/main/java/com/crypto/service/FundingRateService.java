package com.crypto.service;

import com.crypto.entity.MarketType;

public interface FundingRateService {
   
    public void backFillFundingRate(MarketType marketType);
    public void syncFundingRate(String symbol, MarketType marketType);
}

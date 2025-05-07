package com.crypto.service;

import com.crypto.entity.MarketType;

public interface OhlcvService {

    void backfillOhlcv(String symbol, String interval, MarketType marketType, long toTimestamp);
}

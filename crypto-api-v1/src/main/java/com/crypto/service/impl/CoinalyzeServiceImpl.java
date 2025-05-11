package com.crypto.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.clients.CoinalyzeClient;
import com.crypto.dto.CoinalyzeMarketInfoDTO;
import com.crypto.entity.MarketType;
import com.crypto.service.CoinalyzeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinalyzeServiceImpl implements CoinalyzeService {

    private final CoinalyzeClient client;

    @Override
    public List<CoinalyzeMarketInfoDTO> fetchMarketData(MarketType marketType) {
       return client.getExchangeMapping(marketType);
    }
    
}

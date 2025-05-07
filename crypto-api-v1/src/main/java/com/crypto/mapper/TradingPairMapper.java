package com.crypto.mapper;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.crypto.clients.response.BinanceSymbolInfo;
import com.crypto.dto.TradingPairDTO;
import com.crypto.entity.MarketType;

@Component
public class TradingPairMapper {

    public List<TradingPairDTO> mapToDTOList(List<BinanceSymbolInfo> symbols, MarketType marketType) {
        return symbols.stream()
                .filter(s -> "TRADING".equalsIgnoreCase(s.getStatus()) && "USDT".equalsIgnoreCase(s.getQuoteAsset()))
                .map(s -> TradingPairDTO.builder()
                        .symbol(s.getSymbol())
                        .baseAsset(s.getBaseAsset())
                        .quoteAsset(s.getQuoteAsset())
                        .marketType(marketType)
                        .contractType(s.getContractType())     // null for SPOT
                        .marginAsset(s.getMarginAsset())       // null for SPOT
                        .pricePrecision(s.getPricePrecision())
                        .quantityPrecision(s.getQuantityPrecision())
                        .isActive(true)
                        .build())
                .collect(Collectors.toList());
    }
}

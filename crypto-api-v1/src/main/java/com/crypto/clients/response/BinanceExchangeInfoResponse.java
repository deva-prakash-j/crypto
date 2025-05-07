package com.crypto.clients.response;

import lombok.Data;

import java.util.List;

@Data
public class BinanceExchangeInfoResponse {
    private List<BinanceSymbolInfo> symbols;
}
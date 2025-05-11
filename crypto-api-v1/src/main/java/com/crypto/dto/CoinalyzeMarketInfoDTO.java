package com.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinalyzeMarketInfoDTO {
    
    private String symbol;

    private String exchange;

    private String symbolOnExchange;
}

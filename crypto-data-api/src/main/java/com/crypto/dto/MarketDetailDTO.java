package com.crypto.dto;

import lombok.Data;

@Data
public class MarketDetailDTO {
    private String symbol;
    private String pair;
    private String ecode;
    private String base_currency_short_name;
    private String target_currency_short_name;
    private String status;
}

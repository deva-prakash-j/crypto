package com.crypto.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.crypto.dto.SyncDTO;

@FeignClient(name = "crypto-api-v1", url = "${feign.client.config.crypto-data-api}")
public interface CryptoDataApiClient {
    
     @GetMapping("/sync/status")
    public Map<String, SyncDTO> getSyncStatus();
}

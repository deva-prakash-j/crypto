package com.crypto.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.crypto.dto.SyncDTO;
import com.crypto.feign.CryptoDataApiClient;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramHandlerService {

    private final TelegramNotifier notifier;
    private final CryptoDataApiClient apiClient;

    public void handleMessage(JsonNode message) {
        if (message == null || !message.has("text")) return;

        String text = message.get("text").asText().toLowerCase();
        long chatId = message.get("chat").get("id").asLong();

        switch (text) {
            case "/syncstatus" -> notifier.sendMessage(chatId, generateSyncStatus());
            case "/hello" -> notifier.sendMessage(chatId, "👋 Hello! I'm your sync bot.");
            default -> notifier.sendMessage(chatId, "❓ Unknown command. Try /syncstatus");
        }
    }

    private String generateSyncStatus() {
        Map<String, SyncDTO> syncMap = apiClient.getSyncStatus();
        StringBuilder sb = new StringBuilder("📊 *Sync Status (IST)*\n");
    
        for (Map.Entry<String, SyncDTO> entry : syncMap.entrySet()) {
            String symbol = entry.getKey();
            SyncDTO dto = entry.getValue();
    
            sb.append("\n*").append(symbol).append("*\n")
              .append("`  - OHLCV 1m:   ").append(dto.getOhlcv1mSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOhlcv1mSyncedAt())).append(")`\n")
              .append("`  - OHLCV 1h:   ").append(dto.getOhlcv1hSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOhlcv1hSyncedAt())).append(")`\n")
              .append("`  - OHLCV 1d:   ").append(dto.getOhlcv1dSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOhlcv1dSyncedAt())).append(")`\n")
              .append("`  - Funding:    ").append(dto.getFundingRateSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getFundingRateSyncedAt())).append(")`\n")
              .append("`  - OI 5m:      ").append(dto.getOpenInterest5mSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOpenInterest5mSyncedAt())).append(")`\n")
              .append("`  - OI 1h:      ").append(dto.getOpenInterest1hSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOpenInterest1hSyncedAt())).append(")`\n")
              .append("`  - OI 1d:      ").append(dto.getOpenInterest1dSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOpenInterest1dSyncedAt())).append(")`\n")
              .append("`  - OrderBook:  ").append(dto.getOrderBookSyncedAt())
                  .append(" (").append(formatTimeAgo(dto.getOrderBookSyncedAt())).append(")`\n");
        }
    
        return sb.toString();
    }
    
    

    private String formatTimeAgo(LocalDateTime syncedAt) {
        Instant syncInstant = syncedAt.atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Duration duration = Duration.between(syncInstant, Instant.now());

        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + " min ago";

        long hours = duration.toHours();
        if (hours < 24) return hours + " hr ago";

        long days = duration.toDays();
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }



}


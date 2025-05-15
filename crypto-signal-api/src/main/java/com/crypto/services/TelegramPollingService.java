package com.crypto.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramPollingService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private long lastUpdateId = 0;

    private final TelegramHandlerService handlerService;

    @Scheduled(fixedDelay = 1000) // every 1 seconds
    public void pollTelegram() {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1);

            String response = new RestTemplate().getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode updates = mapper.readTree(response).get("result");

            for (JsonNode update : updates) {
                lastUpdateId = update.get("update_id").asLong();
                handlerService.handleMessage(update.get("message"));
            }
        } catch (Exception e) {
            System.err.println("❌ Polling error: " + e.getMessage());
        }
    }
}

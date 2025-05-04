package com.crypto.service;

import com.crypto.dto.CandleDTO;
import com.crypto.entity.OhlcvData;
import com.crypto.repository.OhlcvDataRepository;
import com.crypto.repository.OhlcvSyncTrackerRepository;
import com.crypto.entity.OhlcvSyncTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OhlcvSyncService {

    private final OhlcvFetcherService fetcherService;
    private final OhlcvDataRepository ohlcvDataRepository;
    private final OhlcvSyncTrackerRepository trackerRepository;

    // 🕐 Every hour: sync 1m and 1h
    //@Scheduled(cron = "0 0 * * * *") // hourly
    public void syncMinuteAndHourly() {
        log.info("Scheduled sync started: 1m + 1h");
        syncByInterval("1m");
        syncByInterval("1h");
    }

    // 🕛 Every day at midnight: sync 1d
    //@Scheduled(cron = "0 15 0 * * *") // daily
    public void syncDaily() {
        log.info("Scheduled sync started: 1d");
        syncByInterval("1d");
    }

    private void syncByInterval(String interval) {
        List<OhlcvSyncTracker> trackers = trackerRepository.findAllByInterval(interval);
        for (OhlcvSyncTracker tracker : trackers) {
            try {
                syncToken(tracker);
            } catch (Exception e) {
                log.warn("Failed syncing {}@{}: {}", tracker.getPair(), interval, e.getMessage());
            }
        }
    }

    private void syncToken(OhlcvSyncTracker tracker) {
        String pair = tracker.getPair();
        String interval = tracker.getInterval();
        LocalDateTime from = tracker.getLastSyncedAt();

        String instrument = convertToInstrument(pair); // e.g. BTCUSDT → BTC-USD
        long fromTs = from.toEpochSecond(ZoneOffset.UTC);
        long toTs = Instant.now().getEpochSecond();

        List<CandleDTO> candles = fetcherService.fetchBackfill(instrument, interval, fromTs, toTs);
        if (candles.isEmpty()) {
            log.info("No new candles for {} @ {}", pair, interval);
            return;
        }

        List<OhlcvData> ohlcvRecords = candles.stream()
                .map(c -> fetcherService.mapToEntity(c, pair, interval))
                .toList();

        ohlcvDataRepository.saveAll(ohlcvRecords);

        // Update tracker with latest timestamp
        LocalDateTime newSyncTime = ohlcvRecords.stream()
                .map(OhlcvData::getTimestamp)
                .max(Comparator.naturalOrder())
                .orElse(from);

        trackerRepository.updateLastSynced(pair, interval, newSyncTime);
        log.info("Synced {} candles for {} @ {}. New lastSyncedAt: {}", ohlcvRecords.size(), pair, interval, newSyncTime);
    }

    private String convertToInstrument(String pair) {
        if (pair.endsWith("USDT")) return pair.replace("USDT", "") + "-USD";
        if (pair.endsWith("BTC")) return pair.replace("BTC", "") + "-BTC";
        if (pair.endsWith("INR")) return pair.replace("INR", "") + "-INR";
        return pair; // fallback
    }
}

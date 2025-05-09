package com.crypto.scheduler;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvBackfillTracker;
import com.crypto.repository.OhlcvBackfillTrackerRepository;
import com.crypto.service.OhlcvService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class OhlcvSyncScheduler {
    
    private OhlcvBackfillTrackerRepository backfillTrackerRepository; 
    private BinanceWebClientDTO binanceWebClientDTO;
    private final OhlcvService ohlcvService;

    @PostConstruct
    public void init() {
        log.info("Initiated OhlcvSyncScheduler");
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void syncEveryFiveMinutes() {
        List<MarketType> supportedMarkets = binanceWebClientDTO.getSupportedMarkets();
        List<String> supportedIntervals = List.of("1m", "5m");
        long now = System.currentTimeMillis();

        for(MarketType type: supportedMarkets) {
            for(String interval: supportedIntervals) {
                List<OhlcvBackfillTracker> trackers = fetchSymbols(interval, type.name());
                if(trackers != null && !trackers.isEmpty()) {
                    for(OhlcvBackfillTracker tracker: trackers) {
                        try {
                            log.info("Triggering backfill for {} {} {}", tracker.getSymbol(), type, interval);
                            ohlcvService.backfillOhlcv(tracker.getSymbol(), interval, type, now);
                        } catch (Exception e) {
                            log.error("❌ Failed backfill for {} {} {}", tracker.getSymbol(), type, interval, e);
                        }
                    }
                }
            }
        }
    }

    private List<OhlcvBackfillTracker> fetchSymbols(String marketType, String interval) {
        return backfillTrackerRepository.findByMarketTypeAndInterval(interval, marketType);
    } 

}

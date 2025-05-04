package com.crypto.service;

import com.crypto.dto.CandleDTO;
import com.crypto.entity.ActiveToken;
import com.crypto.entity.OhlcvData;
import com.crypto.repository.ActiveTokenRepository;
import com.crypto.repository.OhlcvDataRepository;
import com.crypto.repository.OhlcvSyncTrackerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OhlcvBackfillService {

    private final OhlcvFetcherService fetcherService;
    private final OhlcvDataRepository ohlcvDataRepository;
    private final OhlcvSyncTrackerRepository trackerRepository;
    private final ActiveTokenRepository activeTokenRepository;

    /**
     * Backfill candles for a pair+interval from as far back as possible to now.
     */
    @Transactional
    public void backfill(String instrument, String interval) {
        log.info("Starting backfill for {} @ {}", instrument, interval);

        long toTs = Instant.now().getEpochSecond();
        boolean done = false;

        LocalDateTime firstAvailable = null;
        LocalDateTime lastSynced = null;

        int totalSaved = 0;

        while (!done) {
            List<CandleDTO> batch = fetcherService.fetchBackfill(instrument, interval, 0, toTs);

            if (batch.isEmpty()) break;

            List<OhlcvData> entities = new ArrayList<>();
            for (CandleDTO c : batch) {
                OhlcvData data = fetcherService.mapToEntity(c, instrument, interval);
                entities.add(data);

                LocalDateTime candleTime = data.getTimestamp();
                if (firstAvailable == null || candleTime.isBefore(firstAvailable)) {
                    firstAvailable = candleTime;
                }
                if (lastSynced == null || candleTime.isAfter(lastSynced)) {
                    lastSynced = candleTime;
                }
            }

            ohlcvDataRepository.saveAll(entities);
            totalSaved += entities.size();
            log.info("Backfilled {} candles so far for {} @ {}", totalSaved, instrument, interval);

            long oldest = batch.stream().mapToLong(CandleDTO::getTimestamp).min().orElse(toTs * 1000);
            toTs = (oldest / 1000) - 1;

            done = batch.size() < 2000;
        }

        if (firstAvailable != null && lastSynced != null) {
            trackerRepository.upsertTracker(instrument, interval, lastSynced, firstAvailable);
            log.info("Backfill complete for {} @ {}. First: {}, Last: {}, Total: {}", instrument, interval, firstAvailable, lastSynced, totalSaved);
        } else {
            log.warn("No data found for {} @ {} during backfill", instrument, interval);
        }
    }
    
    public void backFillDayData() {
    	List<ActiveToken> list = activeTokenRepository.findPairToBackfillDay();
    	for(ActiveToken entity: list) {
    		try {
    			backfill(entity.getPair(), "1d");
    			entity.setBackfillDay(true);
    			activeTokenRepository.save(entity);
    		} catch (Exception e) {
				log.error("Exception while backfilling pair {}", entity.getPair());
			}
    	}
    }
}

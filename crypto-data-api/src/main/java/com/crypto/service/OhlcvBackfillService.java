package com.crypto.service;

import com.crypto.dto.CandleDTO;
import com.crypto.entity.ActiveToken;
import com.crypto.entity.OhlcvData;
import com.crypto.entity.OhlcvSyncTracker;
import com.crypto.repository.ActiveTokenRepository;
import com.crypto.repository.OhlcvDataRepository;
import com.crypto.repository.OhlcvSyncTrackerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final OhlcvDataService dataService;

    /**
     * Backfill candles for a pair+interval from as far back as possible to now.
     */
    @Transactional
    public void backfillDayCandle(String instrument, String interval) {
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

            //ohlcvDataRepository.saveAll(entities);
            dataService.bulkInsertIgnoreConflicts(entities);
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
    
    //@Transactional
    public void backfill(String instrument, String interval, LocalDateTime firstAvailableAt, LocalDateTime lastSyncedAt) {
    	log.info("Starting backfill for {} @ {}", instrument, interval);
    	
    	 LocalDateTime firstAvailable = firstAvailableAt;
         LocalDateTime lastSynced = null;
         boolean done = false;
         LocalDateTime fetchTill = lastSyncedAt;
         long toTs;
    	
    	
       int totalSaved = 0;
       
       while (!done) {
    	   fetchTill = switch (interval) {
           case "1h" -> fetchTill.plusHours(2000);
           case "1m" -> fetchTill.plusMinutes(2000);
           default -> throw new IllegalArgumentException("Invalid interval: " + interval);
          };
          
          if(LocalDateTime.now().isBefore(fetchTill)) {
        	  toTs = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        	  done = true;
          } else {
        	  toTs = fetchTill.toEpochSecond(ZoneOffset.UTC);
          }
          
          List<CandleDTO> batch = fetcherService.fetchBackfillAll(instrument, interval, 0, toTs);
          
          if (batch == null || batch.isEmpty()) break;

          List<OhlcvData> entities = new ArrayList<>();
          for (CandleDTO c : batch) {
              OhlcvData data = fetcherService.mapToEntity(c, instrument, interval);
              entities.add(data);

              LocalDateTime candleTime = data.getTimestamp();
              if (lastSynced == null || candleTime.isAfter(lastSynced)) {
                  lastSynced = candleTime;
              }
          }
          
          //ohlcvDataRepository.saveAll(entities);
          dataService.bulkInsertIgnoreConflicts(entities);
          totalSaved += entities.size();
          log.info("Backfilled {} candles so far for {} @ {}", totalSaved, instrument, interval);
          
          trackerRepository.upsertTracker(instrument, interval, lastSynced, firstAvailable);
          log.info("Backfill complete for {} @ {}. First: {}, Last: {}, Total: {}", instrument, interval, firstAvailable, lastSynced, totalSaved);
       }
    	
    }
    
    public void backFillDayData() {
    	List<ActiveToken> list = activeTokenRepository.findPairToBackfillDay();
    	for(ActiveToken entity: list) {
    		try {
    			backfillDayCandle(entity.getPair(), "1d");
    			entity.setBackfillDay(true);
    			activeTokenRepository.save(entity);
    		} catch (Exception e) {
				log.error("Exception while backfilling pair {}", entity.getPair());
			}
    	}
    }
    
    
    public void backFillHourData() {
    	List<ActiveToken> list = activeTokenRepository.findPairToBackfillMinute();
    	List<OhlcvSyncTracker> trackerList = trackerRepository.findAllByInterval("1d");
    	List<OhlcvSyncTracker> trackerListHour = trackerRepository.findAllByInterval("1m");
    	LocalDateTime fetchFrom;
    	for(ActiveToken entity: list) {
    		try {
    			var trackerOptional =
    			trackerList.stream().filter(t -> t.getPair().equals(entity.getPair())).findFirst();
    			var hourTrackerOptional =
    					trackerListHour.stream().filter(t -> t.getPair().equals(entity.getPair())).findFirst();
    			
    			if(trackerOptional.isEmpty()) continue;
    			
    			var tracker = trackerOptional.get();
    			fetchFrom = tracker.getFirstAvailableAt();
    			
    			if(hourTrackerOptional.isPresent()) {
    				LocalDateTime lastSyncedAt = hourTrackerOptional.get().getLastSyncedAt();
    				fetchFrom = lastSyncedAt != null ? lastSyncedAt : fetchFrom;
    			}
    			
    			backfill(entity.getPair(), "1m", tracker.getFirstAvailableAt(), fetchFrom);
    			entity.setBackfillHour(true);
    			activeTokenRepository.save(entity);
    		} catch (Exception e) {
				log.error("Exception while backfilling pair {} & {}", entity.getPair(), e.getLocalizedMessage());
			}
    	}
    }
}

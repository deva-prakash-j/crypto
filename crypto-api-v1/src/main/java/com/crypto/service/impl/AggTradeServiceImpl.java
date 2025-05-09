package com.crypto.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.AggTrade;
import com.crypto.entity.AggTradeSyncTracker;
import com.crypto.entity.MarketType;
import com.crypto.repository.AggTradeRepository;
import com.crypto.repository.AggTradeSyncTrackerRepository;
import com.crypto.service.AggTradeService;
import com.crypto.util.CommonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggTradeServiceImpl implements AggTradeService {
    
    private final AggTradeRepository aggTradeRepository;
    private final AggTradeSyncTrackerRepository syncTrackerRepository;
    private final BinanceWebClientDTO binanceWebClientDTO;

    public void syncAggTradeData(String symbol, MarketType type) {
        LocalDate start = syncTrackerRepository.findById(symbol)
                .map(AggTradeSyncTracker::getLastSyncedDate)
                .map(date -> date.plusDays(1))
                .orElse(findFirstAvailableFile(symbol, LocalDate.of(2019, 1, 1), LocalDate.now()));

        LocalDate today = LocalDate.now();

        while (!start.isAfter(today)) {
            String dateStr = start.toString();
            String fileName = symbol + "-aggTrades-" + dateStr + ".zip";
            String url = binanceWebClientDTO.getAggTradeHost() + symbol + "/" + fileName;

            try (InputStream zipStream = new BufferedInputStream(new URL(url).openStream());
                ZipInputStream zis = new ZipInputStream(zipStream)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.getName().endsWith(".csv")) continue;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    String line;
                    boolean firstLine = true;
                    List<AggTrade> batch = new ArrayList<>();

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) { firstLine = false; continue; }

                        String[] parts = line.split(",");
                        if (parts.length < 4) continue;

                        AggTrade depth =  AggTrade.builder()
                            .symbol(symbol)
                            .marketType(type)
                            .aggregateTradeId(Long.parseLong(parts[0]))
                            .price(new BigDecimal(parts[1]))
                            .quantity(new BigDecimal(parts[2]))
                            .firstTradeId(Long.parseLong(parts[3]))
                            .lastTradeId(Long.parseLong(parts[4]))
                            .timestamp(Long.parseLong(parts[5]))
                            .isBuyerMaker(Boolean.parseBoolean(parts[6]))
                            .build();

                        batch.add(depth);
                    }

                    aggTradeRepository.saveAll(batch);
                    syncTrackerRepository.save(AggTradeSyncTracker.builder().symbol(symbol).marketType(type).lastSyncedDate(start).build());
                    System.out.println("✅ Synced: " + symbol + " - " + dateStr);
                }

            } catch (FileNotFoundException e) {
                System.out.println("⚠️ File not found for " + url + " — skipping.");
            } catch (Exception e) {
                System.err.println("❌ Error processing " + fileName + ": " + e.getMessage());
                break;
            }

            start = start.plusDays(1);
        }
    }

    private LocalDate findFirstAvailableFile(String symbol, LocalDate start, LocalDate end) {
        LocalDate low = start;
        LocalDate high = end;
        LocalDate firstAvailable = null;

        while (!low.isAfter(high)) {
            LocalDate mid = low.plusDays(ChronoUnit.DAYS.between(low, high) / 2);
            String fileName = symbol + "-aggTrades-" + mid + ".zip";
            String url = binanceWebClientDTO.getAggTradeHost() + symbol + "/" + fileName;

            boolean exists = CommonUtil.fileExistsOnRemote(url);
            if (exists) {
                firstAvailable = mid;
                high = mid.minusDays(1); // try earlier
            } else {
                low = mid.plusDays(1);   // go later
            }
        }

        return firstAvailable;
    }
}

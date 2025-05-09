package com.crypto.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.entity.OrderBookDepth;
import com.crypto.entity.OrderBookDepthSyncTracker;
import com.crypto.repository.OrderBookDepthRepository;
import com.crypto.repository.OrderBookDepthSyncTrackerRepository;
import com.crypto.service.OrderBookDepthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBookDepthServiceImpl implements OrderBookDepthService{

    private final BinanceWebClientDTO binanceWebClientDTO;
    private final OrderBookDepthSyncTrackerRepository syncTrackerRepository;
    private final OrderBookDepthRepository bookDepthRepository;

    public void syncBookDepth(String symbol, MarketType type) {
        LocalDate start = syncTrackerRepository.findById(symbol)
                .map(OrderBookDepthSyncTracker::getLastSyncedDate)
                .map(date -> date.plusDays(1))
                .orElse(LocalDate.of(2023, 1, 1));

        LocalDate today = LocalDate.now();

        while (!start.isAfter(today)) {
            String dateStr = start.toString();
            String fileName = symbol + "-bookDepth-" + dateStr + ".zip";
            String url = binanceWebClientDTO.getBookDepthHost() + symbol + "/" + fileName;

            try (InputStream zipStream = new BufferedInputStream(new URL(url).openStream());
                ZipInputStream zis = new ZipInputStream(zipStream)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.getName().endsWith(".csv")) continue;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    String line;
                    boolean firstLine = true;
                    List<OrderBookDepth> batch = new ArrayList<>();

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) { firstLine = false; continue; }

                        String[] parts = line.split(",");
                        if (parts.length < 4) continue;

                        OrderBookDepth depth = new OrderBookDepth();
                        depth.setSymbol(symbol);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        depth.setTimestamp(LocalDateTime.parse(parts[0], formatter));
                        depth.setPercentage(Integer.parseInt(parts[1]));
                        depth.setDepth(new BigDecimal(parts[2]));
                        depth.setNotional(new BigDecimal(parts[3]));
                        depth.setMarketType(type);

                        batch.add(depth);
                    }

                    bookDepthRepository.saveAll(batch);
                    syncTrackerRepository.save(new OrderBookDepthSyncTracker(symbol, start, type));
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
    
}

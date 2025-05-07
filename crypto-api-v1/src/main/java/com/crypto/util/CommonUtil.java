package com.crypto.util;

public class CommonUtil {

    public static long getIntervalMillis(String interval) {
        return switch (interval) {
            case "1m" -> 60_000L;
            case "3m" -> 180_000L;
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "30m" -> 1_800_000L;
            case "1h" -> 3_600_000L;
            case "2h" -> 7_200_000L;
            case "4h" -> 14_400_000L;
            case "6h" -> 21_600_000L;
            case "8h" -> 28_800_000L;
            case "12h" -> 43_200_000L;
            case "1d" -> 86_400_000L;
            case "3d" -> 259_200_000L;
            case "1w" -> 604_800_000L;
            case "1M" -> 2_592_000_000L;
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }  
    
}

package com.crypto.util;

import java.net.HttpURLConnection;

import java.net.URL;

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
    
    public static String mapInterval(String interval) {
        return switch (interval) {
            case "1min" -> "1m";
            case "5min" -> "5m";
            case "15min" -> "15m";
            case "30min" -> "30m";
            case "1hour" -> "1h";
            case "2hour" -> "2h";
            case "4hour" -> "4h";
            case "6hour" -> "6h";
            case "12hour" -> "12h";
            case "daily" -> "1d";
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }

    public static boolean fileExistsOnRemote(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            System.err.println("❌ Error checking file " + urlStr + ": " + e.getMessage());
            return false;
        }
    }
    
}

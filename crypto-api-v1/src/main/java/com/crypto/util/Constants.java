package com.crypto.util;

public class Constants {

	public static final String ACCEPT = "Accept";
	public static final String APPLICATION_JSON = "application/json";
	public static final String API_KEY = "api_key";
	
	public final static String REDIS_KEY_PREFIX = "trading_pairs:";
	public final static long TTL_HOURS = 24;

	//request types
	public final static String FUNDING_RATE = "funding_rate";
	public final static String OPEN_INTEREST = "open_interest";
	public final static String LIQUIDATION_DATA = "liquidation_data";
	public final static String TOP_LONG_SHORT = "top_long_short";
	public final static String GLOBAL_LONG_SHORT = "global_long_short";
}

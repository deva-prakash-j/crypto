package com.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.crypto.service.OhlcvBackfillService;
import com.crypto.service.TokenDiscoveryService;

@SpringBootApplication
public class CryptoDataApiApplication implements CommandLineRunner {
	
	@Autowired
	private TokenDiscoveryService discoveryService;
	
	@Autowired
	private OhlcvBackfillService backfillService;

	public static void main(String[] args) {
		SpringApplication.run(CryptoDataApiApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Hello");
		System.out.println(discoveryService.getMarketData());
		System.out.println(discoveryService.getMarketData().size());
		backfillService.backFillDayData();
	}

}

import pandas as pd
from sqlalchemy import create_engine

# PostgreSQL connection (replace with your credentials)
DB_CONFIG = {
    "user": "crypto",
    "password": "crypto-signal",
    "host": "srv814560.hstgr.cloud",
    "port": 5432,
    "database": "crypto"
}
conn_str = f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@" \
               f"{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
engine = create_engine(conn_str)

# Symbols and intervals
symbols = ["BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT", "DOGEUSDT"]
intervals = ["1m", "5m", "15m", "30m", "1h", "1d"]
market_type = "FUTURES_USDT"

# Create directory for Parquet files
import os
if not os.path.exists("data"):
    os.makedirs("data")

# Fetch data for each symbol and interval
for symbol in symbols:
    for interval in intervals:
        query = f"""
            SELECT *
            FROM ohlcv_data
            WHERE symbol = '{symbol}'
            AND interval = '{interval}'
            AND market_type = '{market_type}'
            ORDER BY open_time
        """
        # Read data into a DataFrame
        df = pd.read_sql(query, engine)
        
        # Skip empty results
        if df.empty:
            print(f"No data for {symbol}, {interval}, {market_type}")
            continue
        
        # Save to Parquet
        output_path = f"data/{symbol}_{market_type}_{interval}.parquet"
        df.to_parquet(output_path, index=False)
        print(f"Saved {symbol}, {interval}, {market_type} to Parquet")
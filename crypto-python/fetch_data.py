import pandas as pd
from sqlalchemy import create_engine

# === CONFIGURATION === #
DB_CONFIG = {
    "user": "crypto",
    "password": "crypto-signal",
    "host": "srv814560.hstgr.cloud",
    "port": 5432,
    "database": "crypto"
}

# === DATABASE CONNECTION === #
def get_engine():
    conn_str = f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@" \
               f"{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
    return create_engine(conn_str)

# === GET SYMBOLS === #
def fetch_symbols():
    query = """
        SELECT DISTINCT symbol FROM ohlcv_data
        WHERE market_type = 'FUTURES_USDT'
    """
    return pd.read_sql(query, get_engine())['symbol'].tolist()

# === FETCH OHLCV === #
def fetch_ohlcv(symbol, interval):
    query = f"""
        SELECT open_time, open, high, low, close, volume
        FROM ohlcv_data
        WHERE symbol = '{symbol}' AND interval = '{interval}' AND market_type = 'FUTURES_USDT'
        ORDER BY open_time ASC
    """
    df = pd.read_sql(query, get_engine())
    df['timestamp'] = pd.to_datetime(df['open_time'], unit='ms')
    df.set_index('timestamp', inplace=True)
    return df

import pandas as pd
from ta.trend import SMAIndicator, MACD
from ta.momentum import RSIIndicator

symbols = ["BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT", "DOGEUSDT"]
intervals = ["1m", "5m", "15m", "30m", "1h", "1d"]
market_type = "FUTURES_USDT"

# Function to preprocess a single Parquet file
def preprocess_parquet(symbol, interval, market_type):
    # Load Parquet file
    df = pd.read_parquet(f"data/{symbol}_{market_type}_{interval}.parquet")

    # Handle missing values
    df.fillna(method='ffill', inplace=True)

    # Add technical indicators
    df['MA_5'] = SMAIndicator(df['close'], window=5).sma_indicator()
    df['MA_10'] = SMAIndicator(df['close'], window=10).sma_indicator()
    df['RSI'] = RSIIndicator(df['close'], window=14).rsi()
    macd = MACD(df['close'])
    df['MACD'] = macd.macd()
    df['MACD_Signal'] = macd.macd_signal()

    # Add features from additional columns
    df['Taker_Buy_Ratio'] = df['taker_buy_volume'] / df['volume']  # Buying pressure
    df['Trade_Volume_Ratio'] = df['quote_volume'] / df['volume']  # Quote to base volume ratio
    df['Volatility'] = df['close'].rolling(window=20).std()  # 20-period volatility

    # Create target variable (1 if next close > current close, else 0)
    df['Target'] = (df['close'].shift(-1) > df['close']).astype(int)

    # Drop rows with NaN (due to indicators or target)
    df.dropna(inplace=True)

    # Save preprocessed data
    df.to_parquet(f"data/preprocessed_{symbol}_{market_type}_{interval}.parquet", index=False)
    print(f"Preprocessed {symbol}, {interval}, {market_type}")

# Preprocess all files
for symbol in symbols:
    for interval in intervals:
        preprocess_parquet(symbol, interval, market_type)
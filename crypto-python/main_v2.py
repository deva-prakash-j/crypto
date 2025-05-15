import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LinearRegression
from sqlalchemy import create_engine
from ta.momentum import RSIIndicator
from ta.trend import MACD, EMAIndicator
from ta.volatility import BollingerBands, AverageTrueRange

from fetch_data import fetch_ohlcv, fetch_symbols

# === CONFIGURATION === #
INTERVAL = "1h"
MARKET_TYPE = "spot"
FUTURE_WINDOW_HOURS = 24
THRESHOLD = 0.02
STOP_LOSS_PCT = 0.015
MAX_LEVERAGE = 5
LIVE_LOOKBACK = 100
MIN_FEATURE_IMPORTANCE = 0.001

BASE_FEATURE_COLUMNS = [
    'return', 'rsi', 'ema_fast', 'ema_slow', 'macd',
    'bb_upper', 'bb_lower', 'volatility', 'momentum',
    'rolling_max', 'rolling_min', 'volume_surge',
    'body_size', 'upper_wick', 'lower_wick', 'candle_range',
    'ema_ratio', 'atr', 'close_to_high', 'close_to_rolling_max',
    'rsi_15m', 'ema_fast_15m', 'rsi_1d', 'ema_fast_1d'
]

# === ENRICHED FEATURES === #
def add_features(df):
    df['return'] = df['close'].pct_change()
    df['rsi'] = RSIIndicator(df['close']).rsi()
    df['ema_fast'] = EMAIndicator(df['close'], window=12).ema_indicator()
    df['ema_slow'] = EMAIndicator(df['close'], window=26).ema_indicator()
    df['macd'] = MACD(df['close']).macd()
    bb = BollingerBands(df['close'])
    df['bb_upper'] = bb.bollinger_hband()
    df['bb_lower'] = bb.bollinger_lband()
    df['volatility'] = df['close'].rolling(window=14).std()
    df['momentum'] = df['close'] - df['close'].shift(10)
    df['rolling_max'] = df['close'].rolling(window=20).max()
    df['rolling_min'] = df['close'].rolling(window=20).min()
    df['volume_surge'] = df['volume'] / df['volume'].rolling(window=14).mean()
    df['body_size'] = abs(df['close'] - df['open'])
    df['upper_wick'] = df['high'] - df[['close', 'open']].max(axis=1)
    df['lower_wick'] = df[['close', 'open']].min(axis=1) - df['low']
    df['candle_range'] = df['high'] - df['low']
    df['ema_ratio'] = df['ema_fast'] / df['ema_slow']
    df['atr'] = AverageTrueRange(df['high'], df['low'], df['close']).average_true_range()
    df['close_to_high'] = (df['close'] - df['low']) / (df['high'] - df['low']).replace(0, np.nan)
    df['close_to_rolling_max'] = df['close'] / df['rolling_max']
    return df.dropna()

# === MERGE MULTI-TIMEFRAME FEATURES === #
def add_multitimeframe_features(df_1h, df_15m, df_1d):
    df_15m_features = pd.DataFrame(index=df_15m.index)
    df_15m_features['rsi_15m'] = RSIIndicator(df_15m['close']).rsi()
    df_15m_features['ema_fast_15m'] = EMAIndicator(df_15m['close'], window=12).ema_indicator()
    df_1d_features = pd.DataFrame(index=df_1d.index)
    df_1d_features['rsi_1d'] = RSIIndicator(df_1d['close']).rsi()
    df_1d_features['ema_fast_1d'] = EMAIndicator(df_1d['close'], window=12).ema_indicator()
    df = df_1h.copy()
    df = df.merge(df_15m_features, left_index=True, right_index=True, how='left')
    df = df.merge(df_1d_features, left_index=True, right_index=True, how='left')
    return df.ffill().dropna()

# === LABELS === #
def add_labels(df, future_window=FUTURE_WINDOW_HOURS):
    future_max = df['close'].rolling(window=future_window).max().shift(-future_window)
    future_min = df['close'].rolling(window=future_window).min().shift(-future_window)
    df['target'] = 0
    df.loc[(future_max / df['close'] - 1) >= THRESHOLD, 'target'] = 1
    df.loc[(1 - future_min / df['close']) >= THRESHOLD, 'target'] = -1
    return df.dropna()

# === BACKTEST === #
def backtest_signals(df, model, features):
    df = df.copy()
    available = [f for f in features if f in df.columns]
    df = df[available + ['close', 'high', 'low']]
    predictions = model.predict(df[available])
    probs = model.predict_proba(df[available])
    results = []
    for i in range(len(df) - FUTURE_WINDOW_HOURS):
        pred = predictions[i]
        if pred == 0:
            continue
        entry_price = df.iloc[i]['close']
        future_window = df.iloc[i+1:i+FUTURE_WINDOW_HOURS+1]
        direction = "LONG" if pred == 1 else "SHORT"
        confidence = max(probs[i])
        tp_price = entry_price * (1 + THRESHOLD) if direction == "LONG" else entry_price * (1 - THRESHOLD)
        sl_price = entry_price * (1 - STOP_LOSS_PCT) if direction == "LONG" else entry_price * (1 + STOP_LOSS_PCT)
        exit_price = future_window.iloc[-1]['close']
        for _, row in future_window.iterrows():
            high, low = row['high'], row['low']
            if direction == "LONG":
                if high >= tp_price: exit_price = tp_price; break
                if low <= sl_price: exit_price = sl_price; break
            else:
                if low <= tp_price: exit_price = tp_price; break
                if high >= sl_price: exit_price = sl_price; break
        pnl = (exit_price - entry_price) / entry_price if direction == "LONG" else (entry_price - exit_price) / entry_price
        correct = pnl > 0
        results.append({'pnl': pnl, 'correct': correct})
    if not results:
        return {"accuracy": 0, "avg_return_pct": 0, "total_return_pct": 0, "num_trades": 0, "success_trades": 0, "failed_trades": 0}
    pnl_list = [r['pnl'] for r in results]
    return {
        "accuracy": round(sum(r['correct'] for r in results) / len(results), 4),
        "avg_return_pct": round(np.mean(pnl_list) * 100, 2),
        "total_return_pct": round(np.sum(pnl_list) * 100, 2),
        "num_trades": len(results),
        "success_trades": sum(r['correct'] for r in results),
        "failed_trades": len(results) - sum(r['correct'] for r in results)
    }

# === MAIN === #
if __name__ == '__main__':
    symbols = fetch_symbols()
    training_df = []
    for symbol in symbols:
        try:
            df_1h = fetch_ohlcv(symbol, '1h')
            df_15m = fetch_ohlcv(symbol, '15m')
            df_1d = fetch_ohlcv(symbol, '1d')
            df_1h = add_features(df_1h)
            df_merged = add_multitimeframe_features(df_1h, df_15m, df_1d)
            df_labeled = add_labels(df_merged)
            df_labeled = df_labeled.copy()
            df_labeled['symbol'] = symbol
            training_df.append(df_labeled)
        except Exception as e:
            print(f"Skipping {symbol} due to error: {e}")

    full_df = pd.concat(training_df)
    full_df = pd.get_dummies(full_df, columns=['symbol'])
    symbol_features = [col for col in full_df.columns if col.startswith('symbol_')]
    feature_cols = BASE_FEATURE_COLUMNS + symbol_features

    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(full_df[feature_cols], full_df['target'])

    print("\n\U0001F680 Backtest with Random Forest:")
    results = backtest_signals(full_df, model, feature_cols)
    for k, v in results.items():
        print(f"{k}: {v}")

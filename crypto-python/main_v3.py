import pandas as pd
import numpy as np
# import matplotlib.pyplot as plt
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import GridSearchCV, TimeSeriesSplit # Added GridSearchCV and TimeSeriesSplit
from sqlalchemy import create_engine
from ta.momentum import RSIIndicator
from ta.trend import MACD, EMAIndicator
from ta.volatility import BollingerBands, AverageTrueRange

# Import your data fetching functions
from fetch_data import fetch_symbols, fetch_ohlcv


# === CONFIGURATION === #
INTERVAL = "1h"
MARKET_TYPE = "FUTURES_USDT"
FUTURE_WINDOW_HOURS = 24
THRESHOLD = 0.05
STOP_LOSS_PCT = 0.015
TRAIN_TEST_SPLIT_RATIO = 0.8
# Hyperparameter Tuning Config
PERFORM_HYPERPARAMETER_TUNING = True # Set to False to skip tuning and use default/manual params
N_CV_SPLITS = 3 # Number of cross-validation splits for GridSearchCV / TimeSeriesSplit

BASE_FEATURE_COLUMNS = [
    'return', 'rsi', 'ema_fast', 'ema_slow', 'macd',
    'bb_upper', 'bb_lower', 'volatility', 'momentum',
    'rolling_max', 'rolling_min', 'volume_surge',
    'body_size', 'upper_wick', 'lower_wick', 'candle_range',
    'ema_ratio', 'atr', 'close_to_high', 'close_to_rolling_max',
    'rsi_15m', 'ema_fast_15m', 'rsi_1d', 'ema_fast_1d',
    'trend_slope', 'trend_direction'
]

# === ENRICHED FEATURES === #
def add_features(df):
    df = df.copy()
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
    df['volume_surge'] = df['volume'] / (df['volume'].rolling(window=14).mean() + 1e-9)
    df['body_size'] = abs(df['close'] - df['open'])
    df['upper_wick'] = df['high'] - df[['close', 'open']].max(axis=1)
    df['lower_wick'] = df[['close', 'open']].min(axis=1) - df['low']
    df['candle_range'] = df['high'] - df['low']
    df['ema_ratio'] = df['ema_fast'] / (df['ema_slow'] + 1e-9)
    df['atr'] = AverageTrueRange(df['high'], df['low'], df['close']).average_true_range()
    df['close_to_high'] = (df['close'] - df['low']) / (df['high'] - df['low'] + 1e-9)
    df['close_to_rolling_max'] = df['close'] / (df['rolling_max'] + 1e-9)
    df['trend_slope'] = df['close'].rolling(window=10).apply(
        lambda x: LinearRegression().fit(np.arange(len(x)).reshape(-1, 1), x).coef_[0] if len(x) == 10 and not np.isnan(x).any() and np.isfinite(x).all() else np.nan,
        raw=True
    )
    df['trend_direction'] = np.sign(df['trend_slope'])
    return df.dropna()

# === MERGE MULTI-TIMEFRAME FEATURES === #
def add_multitimeframe_features(df_1h, df_15m, df_1d):
    df_1h = df_1h.copy()
    if not df_15m.empty:
        df_15m_features = pd.DataFrame(index=df_15m.index)
        df_15m_features['rsi_15m'] = RSIIndicator(df_15m['close']).rsi()
        df_15m_features['ema_fast_15m'] = EMAIndicator(df_15m['close'], window=12).ema_indicator()
        df_1h = df_1h.merge(df_15m_features, left_index=True, right_index=True, how='left')
    else:
        df_1h['rsi_15m'] = np.nan
        df_1h['ema_fast_15m'] = np.nan

    if not df_1d.empty:
        df_1d_features = pd.DataFrame(index=df_1d.index)
        df_1d_features['rsi_1d'] = RSIIndicator(df_1d['close']).rsi()
        df_1d_features['ema_fast_1d'] = EMAIndicator(df_1d['close'], window=12).ema_indicator()
        df_1h = df_1h.merge(df_1d_features, left_index=True, right_index=True, how='left')
    else:
        df_1h['rsi_1d'] = np.nan
        df_1h['ema_fast_1d'] = np.nan
    return df_1h.ffill().dropna()

# === LABELS === #
def add_labels(df, future_window=FUTURE_WINDOW_HOURS):
    df = df.copy()
    future_window = max(1, future_window)
    future_max_rolling = df['high'].rolling(window=future_window, min_periods=1).max()
    future_min_rolling = df['low'].rolling(window=future_window, min_periods=1).min()
    df['future_max'] = future_max_rolling.shift(-future_window)
    df['future_min'] = future_min_rolling.shift(-future_window)
    df['target'] = 0
    df.loc[(df['future_max'] / df['close'] - 1) >= THRESHOLD, 'target'] = 1
    df.loc[(1 - df['future_min'] / df['close']) >= THRESHOLD, 'target'] = -1
    return df.dropna(subset=['future_max', 'future_min', 'target'])

# === BACKTEST === #
def backtest_signals(df, model, features_for_model):
    df_copy = df.copy()
    results = []
    if df_copy.empty:
        return {"accuracy": 0, "avg_return_pct": 0, "total_return_pct": 0, "num_trades": 0, "success_trades": 0, "failed_trades": 0}

    for symbol_name, df_sym in df_copy.groupby('symbol'):
        if df_sym.empty: continue
        df_sym_predict_ready = df_sym.reindex(columns=features_for_model, fill_value=0)[features_for_model]
        if df_sym_predict_ready.empty: continue
        predictions = model.predict(df_sym_predict_ready)
        # probs = model.predict_proba(df_sym_predict_ready) # Optional
        df_sym = df_sym.reset_index()
        for i in range(len(df_sym) - FUTURE_WINDOW_HOURS):
            pred = predictions[i]
            if pred == 0: continue
            entry_price = df_sym.iloc[i]['close']
            if pd.isna(entry_price): continue
            trade_execution_window = df_sym.iloc[i+1 : i+1+FUTURE_WINDOW_HOURS]
            if trade_execution_window.empty: continue
            direction = "LONG" if pred == 1 else "SHORT"
            tp_price = entry_price * (1 + THRESHOLD) if direction == "LONG" else entry_price * (1 - THRESHOLD)
            sl_price = entry_price * (1 - STOP_LOSS_PCT) if direction == "LONG" else entry_price * (1 + STOP_LOSS_PCT)
            exit_price = trade_execution_window.iloc[-1]['close']
            exit_reason = "window_end"
            for _, row in trade_execution_window.iterrows():
                high, low = row['high'], row['low']
                if pd.isna(high) or pd.isna(low): continue
                if direction == "LONG":
                    if high >= tp_price: exit_price = tp_price; exit_reason = "tp_hit"; break
                    if low <= sl_price: exit_price = sl_price; exit_reason = "sl_hit"; break
                else:
                    if low <= tp_price: exit_price = tp_price; exit_reason = "tp_hit"; break
                    if high >= sl_price: exit_price = sl_price; exit_reason = "sl_hit"; break
            if pd.isna(exit_price): pnl = 0
            elif direction == "LONG": pnl = (exit_price - entry_price) / entry_price
            else: pnl = (entry_price - exit_price) / entry_price
            correct = pnl > 0
            results.append({'symbol': symbol_name, 'entry_time': df_sym.iloc[i]['timestamp'], 'direction': direction, 'entry_price': entry_price, 'exit_price': exit_price, 'exit_reason': exit_reason, 'pnl_pct': pnl * 100, 'correct': correct})
    if not results: return {"accuracy": 0, "avg_return_pct": 0, "total_return_pct": 0, "num_trades": 0, "success_trades": 0, "failed_trades": 0}
    results_df = pd.DataFrame(results)
    num_trades = len(results_df)
    success_trades = results_df['correct'].sum()
    return {"accuracy": round(success_trades / num_trades if num_trades > 0 else 0, 4), "avg_return_pct": round(results_df['pnl_pct'].mean() if num_trades > 0 else 0, 2), "total_pnl_pct_sum_of_trades": round(results_df['pnl_pct'].sum() if num_trades > 0 else 0, 2), "num_trades": num_trades, "success_trades": success_trades, "failed_trades": num_trades - success_trades}

# === MAIN === #
if __name__ == '__main__':
    symbols_list = fetch_symbols()
    if not symbols_list:
        print("No symbols fetched. Exiting.")
        exit()
    print(f"Fetched symbols: {symbols_list}")

    training_data_per_symbol = []
    for symbol_str in symbols_list:
        try:
            print(f"\nProcessing symbol: {symbol_str}")
            df_1h = fetch_ohlcv(symbol_str, '1h')
            if df_1h.empty or len(df_1h) < 100: print(f"Not enough 1h data for {symbol_str} (found {len(df_1h)}). Skipping."); continue
            df_15m = fetch_ohlcv(symbol_str, '15m')
            df_1d = fetch_ohlcv(symbol_str, '1d')
            df_1h_featured = add_features(df_1h)
            if df_1h_featured.empty: print(f"No features for 1h data of {symbol_str}. Skipping."); continue
            df_merged = add_multitimeframe_features(df_1h_featured, df_15m, df_1d)
            if df_merged.empty: print(f"No data after merging for {symbol_str}. Skipping."); continue
            df_labeled = add_labels(df_merged, future_window=FUTURE_WINDOW_HOURS)
            if df_labeled.empty: print(f"No data after labeling for {symbol_str}. Skipping."); continue
            df_labeled = df_labeled.copy(); df_labeled['symbol'] = symbol_str
            training_data_per_symbol.append(df_labeled)
            print(f"Successfully processed {symbol_str}, {len(df_labeled)} samples.")
        except Exception as e: print(f"Skipping {symbol_str} due to error: {e}"); import traceback; traceback.print_exc()

    if not training_data_per_symbol: print("No data collected. Exiting."); exit()
    full_df_raw = pd.concat(training_data_per_symbol).sort_index()
    if full_df_raw.empty: print("Concatenated DataFrame is empty. Exiting."); exit()

    dummies = pd.get_dummies(full_df_raw['symbol'], prefix='symbol', prefix_sep='_')
    full_df_for_model_with_orig_symbol = pd.concat([full_df_raw.reset_index(), dummies.reset_index(drop=True)], axis=1)
    if 'timestamp' in full_df_for_model_with_orig_symbol.columns:
        full_df_for_model_with_orig_symbol = full_df_for_model_with_orig_symbol.set_index('timestamp')
    elif 'index' in full_df_for_model_with_orig_symbol.columns and isinstance(full_df_for_model_with_orig_symbol['index'].iloc[0], pd.Timestamp):
        full_df_for_model_with_orig_symbol = full_df_for_model_with_orig_symbol.rename(columns={'index': 'timestamp'}).set_index('timestamp')
    else: print("Warning: Could not identify datetime index after concat.")

    actual_base_features = [f for f in BASE_FEATURE_COLUMNS if f in full_df_for_model_with_orig_symbol.columns]
    symbol_dummy_features = [col for col in full_df_for_model_with_orig_symbol.columns if col.startswith('symbol_')]
    model_features = actual_base_features + symbol_dummy_features
    if 'target' in model_features: model_features.remove('target')
    if 'symbol' in model_features: model_features.remove('symbol')
    model_features = [f for f in model_features if f in full_df_for_model_with_orig_symbol.columns]

    if 'target' not in full_df_for_model_with_orig_symbol.columns: print("Error: 'target' column missing."); exit()
    missing_model_features = [f for f in model_features if f not in full_df_for_model_with_orig_symbol.columns]
    if missing_model_features: print(f"Error: Missing model features: {missing_model_features}"); exit()

    X = full_df_for_model_with_orig_symbol[model_features]
    y = full_df_for_model_with_orig_symbol['target']
    split_point = int(len(full_df_for_model_with_orig_symbol) * TRAIN_TEST_SPLIT_RATIO)

    if split_point == 0 or split_point >= len(full_df_for_model_with_orig_symbol) -1 :
        print(f"Warning: Train/Test split not meaningful. Train: {split_point}, Total: {len(full_df_for_model_with_orig_symbol)}.")
        if len(full_df_for_model_with_orig_symbol) < 50 : print("Dataset very small.")

    X_train = X.iloc[:split_point]
    y_train = y.iloc[:split_point]
    test_df_for_backtesting = full_df_for_model_with_orig_symbol.iloc[split_point:].copy()

    if X_train.empty or y_train.empty:
        print("Training data is empty after split. Cannot train model.")
    else:
        print(f"\nTraining model on {len(X_train)} samples (features: {X_train.shape[1]}). Test samples: {len(test_df_for_backtesting)}")

        # --- HYPERPARAMETER TUNING ---
        if PERFORM_HYPERPARAMETER_TUNING:
            print("\nPerforming hyperparameter tuning for RandomForestClassifier...")
            # Define the parameter grid to search
            # You can expand this grid, but be mindful of computation time
            param_grid = {
                'n_estimators': [100, 200],  # Number of trees
                'max_depth': [10, 20, None],    # Max depth of trees
                'min_samples_split': [2, 5, 10], # Min samples to split a node
                'min_samples_leaf': [1, 2, 4],   # Min samples in a leaf node
                'max_features': ['sqrt', 'log2'] # Number of features to consider for best split
            }

            # For time series, TimeSeriesSplit is generally preferred for cross-validation
            # tscv = TimeSeriesSplit(n_splits=N_CV_SPLITS)
            # Using standard k-fold CV here for simplicity/speed. Replace N_CV_SPLITS with tscv for TimeSeriesSplit.
            
            rf_model_for_tuning = RandomForestClassifier(random_state=42, class_weight='balanced', n_jobs=-1)
            
            # Note: For very large X_train, GridSearchCV can be slow.
            # Consider RandomizedSearchCV for faster exploration of a larger parameter space.
            # Scoring can be 'accuracy', 'f1_weighted', 'roc_auc_ovr_weighted', etc.
            grid_search = GridSearchCV(estimator=rf_model_for_tuning,
                                       param_grid=param_grid,
                                       cv=N_CV_SPLITS, # Or tscv for TimeSeriesSplit
                                       n_jobs=-1,      # Use all available cores
                                       verbose=2,      # Prints progress
                                       scoring='accuracy') # Or 'f1_weighted' if classes are imbalanced

            grid_search.fit(X_train, y_train)

            print("\nBest parameters found by GridSearchCV:")
            print(grid_search.best_params_)
            
            # Use the best estimator found by GridSearchCV
            model = grid_search.best_estimator_
            # Note: grid_search.best_estimator_ is already fitted on the whole X_train with the best params
            # using cross-validation. So, an explicit model.fit(X_train, y_train) here is redundant
            # if you use grid_search.best_estimator_ directly for predictions.
            # However, if you instantiate a new model with best_params_, you must fit it.
            # For clarity and explicitness, fitting a new model with best_params:
            # model = RandomForestClassifier(**grid_search.best_params_, random_state=42, class_weight='balanced', n_jobs=-1)
            # model.fit(X_train, y_train)
            # The best_estimator_ is already fitted, so we can use it directly.

        else: # Use default/manual parameters if not tuning
            print("\nSkipping hyperparameter tuning. Using default RandomForestClassifier parameters.")
            model = RandomForestClassifier(n_estimators=100, random_state=42, class_weight='balanced', n_jobs=-1)
            model.fit(X_train, y_train)


        # Feature Importance (from the final model)
        importances = model.feature_importances_
        feature_importance_df = pd.DataFrame({'feature': model_features, 'importance': importances})
        feature_importance_df = feature_importance_df.sort_values(by='importance', ascending=False)
        print("\nTop 15 Feature Importances (from the tuned model):")
        print(feature_importance_df.head(15))
        print(f"\nSum of top 15 importances: {feature_importance_df.head(15)['importance'].sum():.4f}")


        print("\n\U0001F680 Backtest on Test Set:")
        if test_df_for_backtesting.empty:
            print("Test data is empty. Skipping backtest.")
        else:
            print(f"Backtesting on {len(test_df_for_backtesting)} samples.")
            results = backtest_signals(test_df_for_backtesting, model, model_features)
            for k, v in results.items():
                print(f"{k}: {v}")
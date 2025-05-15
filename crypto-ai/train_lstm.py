import numpy as np
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout
import pandas as pd

symbols = ["BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT", "DOGEUSDT"]
intervals = ["1m", "5m", "15m", "30m", "1h", "1d"]
market_type = "FUTURES_USDT"

# Function to train LSTM
def train_lstm(symbol, interval, market_type, time_steps=10):
    # Load preprocessed data
    df = pd.read_parquet(f"data/preprocessed_{symbol}_{market_type}_{interval}.parquet")

    # Features and target
    features = ['open', 'high', 'low', 'close', 'volume', 'MA_5', 'MA_10', 'RSI', 'MACD', 'MACD_Signal', 
                'Taker_Buy_Ratio', 'Trade_Volume_Ratio', 'Volatility']
    X = df[features].values
    y = df['Target'].values

    # Create sequences for LSTM
    def create_sequences(X, y, time_steps):
        Xs, ys = [], []
        for i in range(len(X) - time_steps):
            Xs.append(X[i:(i + time_steps)])
            ys.append(y[i + time_steps])
        return np.array(Xs), np.array(ys)

    X_seq, y_seq = create_sequences(X, y, time_steps)

    # Split data
    X_train_seq, X_test_seq, y_train_seq, y_test_seq = train_test_split(X_seq, y_seq, test_size=0.2, shuffle=False)

    # Build LSTM model
    model = Sequential([
        LSTM(50, return_sequences=True, input_shape=(time_steps, X.shape[1])),
        Dropout(0.2),
        LSTM(50),
        Dropout(0.2),
        Dense(1, activation='sigmoid')
    ])
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

    # Train
    model.fit(X_train_seq, y_train_seq, epochs=20, batch_size=32, validation_split=0.1, verbose=1)

    # Evaluate
    loss, accuracy = model.evaluate(X_test_seq, y_test_seq)
    print(f"LSTM Accuracy for {symbol}, {interval}: {accuracy:.4f}")

    # Save model
    model.save(f"models/lstm_{symbol}_{market_type}_{interval}.h5")

# Train LSTM for specific intervals (e.g., 1m, 5m)
for symbol in symbols:
    for interval in ["1m", "5m"]:  # Focus on shorter intervals
        train_lstm(symbol, interval, market_type)
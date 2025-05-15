from sklearn.svm import SVC
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
import os
import pandas as pd

symbols = ["BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT", "DOGEUSDT"]
intervals = ["1m", "5m", "15m", "30m", "1h", "1d"]
market_type = "FUTURES_USDT"

# Function to train SVM for a single dataset
def train_svm(symbol, interval, market_type):
    # Load preprocessed data
    df = pd.read_parquet(f"data/preprocessed_{symbol}_{market_type}_{interval}.parquet")

    # Features and target
    features = ['open', 'high', 'low', 'close', 'volume', 'MA_5', 'MA_10', 'RSI', 'MACD', 'MACD_Signal', 
                'Taker_Buy_Ratio', 'Trade_Volume_Ratio', 'Volatility']
    X = df[features]
    y = df['Target']

    # Split data (preserve temporal order)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=False)

    # Train SVM
    svm = SVC(kernel='linear', C=1.0, random_state=42)
    svm.fit(X_train, y_train)

    # Evaluate
    y_pred = svm.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f"SVM Accuracy for {symbol}, {interval}: {accuracy:.4f}")
    print(classification_report(y_test, y_pred))

    # Save model
    import joblib
    joblib.dump(svm, f"models/svm_{symbol}_{market_type}_{interval}.pkl")

# Train for all symbols and intervals
if not os.path.exists("models"):
    os.makedirs("models")

for symbol in symbols:
    for interval in intervals:
        train_svm(symbol, interval, market_type)
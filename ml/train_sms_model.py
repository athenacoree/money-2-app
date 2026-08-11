import os
import json
import re
import numpy as np
import tensorflow as tf

# Set random seed for reproducibility
np.random.seed(42)
tf.random.set_seed(42)

# Tokenizer helper
def tokenize(text):
    tokens = re.split(r'[\s:,;\(\)\[\]\!\?\-\+]+', text)
    return [t for t in tokens if t]

def clean_and_parse_float(token):
    try:
        clean = re.sub(r'[^\d\.]', '', token)
        return float(clean)
    except ValueError:
        return None

def build_vocab(dataset, max_vocab_size=2000):
    word_counts = {}
    for item in dataset:
        tokens = tokenize(item["body"])
        for token in tokens:
            word_counts[token] = word_counts.get(token, 0) + 1

    # Sort by frequency
    sorted_words = sorted(word_counts.items(), key=lambda x: x[1], reverse=True)
    vocab = {"<PAD>": 0, "<UNK>": 1}
    for word, count in sorted_words[:max_vocab_size-2]:
        vocab[word] = len(vocab)
    return vocab

def encode_text(tokens, vocab, max_len=64):
    encoded = []
    for t in tokens[:max_len]:
        encoded.append(vocab.get(t, vocab["<UNK>"]))
    # Pad
    while len(encoded) < max_len:
        encoded.append(vocab["<PAD>"])
    return encoded

def prepare_data(dataset, vocab, max_len=64):
    X = []
    y_tipo = []
    y_moneda = []
    y_confianza = []
    y_monto_mask = []

    tipo_map = {"ingreso": 0, "gasto": 1, "sin_clasificar": 2}
    moneda_map = {"CUP": 0, "MLC": 1, "USD": 2}
    confianza_map = {"alta": 0, "media": 1, "baja": 2}

    for item in dataset:
        tokens = tokenize(item["body"])
        X.append(encode_text(tokens, vocab, max_len))

        y_tipo.append(tipo_map[item["tipo"]])
        y_moneda.append(moneda_map[item["moneda"]])
        y_confianza.append(confianza_map[item["confianza"]])

        # Build amount mask of length max_len
        mask = [0.0] * max_len
        monto_val = item["monto"]
        if monto_val > 0:
            for idx, token in enumerate(tokens[:max_len]):
                val = clean_and_parse_float(token)
                if val is not None and abs(val - monto_val) < 1e-5:
                    mask[idx] = 1.0
                    break
        y_monto_mask.append(mask)

    return (
        np.array(X, dtype=np.int32),
        np.array(y_tipo, dtype=np.int32),
        np.array(y_moneda, dtype=np.int32),
        np.array(y_confianza, dtype=np.int32),
        np.expand_dims(np.array(y_monto_mask, dtype=np.float32), -1)
    )

def train_and_export():
    print("Loading dataset...")
    with open("ml/dataset.json", "r", encoding="utf-8") as f:
        dataset = json.load(f)

    print(f"Dataset loaded. Total {len(dataset)} examples.")

    # Build vocabulary
    vocab = build_vocab(dataset)
    vocab_size = len(vocab)
    print(f"Vocabulary size: {vocab_size}")

    # Prepare inputs and targets
    max_len = 64
    X, y_tipo, y_moneda, y_confianza, y_monto_mask = prepare_data(dataset, vocab, max_len)

    # Build Keras Multi-Output 1D CNN Model (Very standard and fully supported in TFLite!)
    print("Building 1D CNN model...")
    inputs = tf.keras.Input(shape=(max_len,), name="input_ids", dtype=tf.int32)
    embedding = tf.keras.layers.Embedding(input_dim=vocab_size, output_dim=32, input_length=max_len)(inputs)

    # 1D Conv layers to capture local context around tokens
    conv = tf.keras.layers.Conv1D(filters=32, kernel_size=3, padding="same", activation="relu")(embedding)
    conv = tf.keras.layers.Conv1D(filters=32, kernel_size=3, padding="same", activation="relu")(conv)

    # Global average pooling for sentence-level tasks
    global_rep = tf.keras.layers.GlobalAveragePooling1D()(conv)

    # Task specific Dense output layers
    out_tipo = tf.keras.layers.Dense(3, activation="softmax", name="tipo")(global_rep)
    out_moneda = tf.keras.layers.Dense(3, activation="softmax", name="moneda")(global_rep)
    out_confianza = tf.keras.layers.Dense(3, activation="softmax", name="confianza")(global_rep)

    # Token-level binary mask output using 1x1 Conv (equivalent to TimeDistributed Dense)
    out_monto_mask = tf.keras.layers.Conv1D(filters=1, kernel_size=1, activation="sigmoid", name="monto_mask")(conv)

    model = tf.keras.Model(
        inputs=inputs,
        outputs={
            "tipo": out_tipo,
            "moneda": out_moneda,
            "confianza": out_confianza,
            "monto_mask": out_monto_mask
        }
    )

    model.compile(
        optimizer="adam",
        loss={
            "tipo": "sparse_categorical_crossentropy",
            "moneda": "sparse_categorical_crossentropy",
            "confianza": "sparse_categorical_crossentropy",
            "monto_mask": "binary_crossentropy"
        },
        metrics={
            "tipo": "accuracy",
            "moneda": "accuracy",
            "confianza": "accuracy",
            "monto_mask": "accuracy"
        }
    )

    model.summary()

    print("Training model...")
    history = model.fit(
        X,
        {
            "tipo": y_tipo,
            "moneda": y_moneda,
            "confianza": y_confianza,
            "monto_mask": y_monto_mask
        },
        epochs=30,
        batch_size=32,
        verbose=1
    )

    print("Saving assets...")
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)

    # Save vocabulary
    vocab_path = os.path.join(assets_dir, "vocab.json")
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)
    print(f"Vocabulary saved to {vocab_path}")

    # Save also in ml/ directory
    with open("ml/vocab.json", "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)

    # Convert model to TFLite
    print("Converting model to TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT] # Post-training dynamic range quantization

    tflite_model = converter.convert()

    tflite_path = os.path.join(assets_dir, "sms_model.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"Quantized TFLite model successfully saved to {tflite_path}")
    print(f"Model size: {len(tflite_model) / 1024.0:.2f} KB (Well below 20-25 MB limits!)")

if __name__ == "__main__":
    train_and_export()

import json
import random
import re

# Set random seed for reproducibility
random.seed(42)

# Entities and Merchants for variations
ENTITIES = [
    "EESEC. YDE SERV. DEPROD.UNIVERSALES LA HABANA",
    "TRD CARIBE",
    "CIMEX PLAZA",
    "CUPET LA RAMPA",
    "AGUAS DE LA HABANA",
    "ETECSA OFICINA CENTRO",
    "GAESA SHOP",
    "MINFAR CENTRAL",
    "TIENDA LA EPOCA",
    "SUPERMERCADO 3RA Y 70",
    "RESTAURANTE EL ALJIBE",
    "PALADARES VEDADO"
]

COMERCIOS_ENZONA = [
    "Tienda Virtual Cimex",
    "Restaurante El Biky",
    "La Casa de la Música",
    "Bazar Habana",
    "Servicentro Tangana",
    "Farmacia Vedado",
    "Cafetería El Rápido",
    "Pizzería Mimosa"
]

NAMES = [
    "Marlon Baez", "Athena Core", "Jules AI", "Luis Alberto", "María Elena",
    "Alejandro Gómez", "Yusniel Torres", "Patricia Blanco", "Carlos Manuel",
    "Yoelvis Pérez", "Yanet Fraga", "Jorge Luis", "Roberto Fontanills"
]

CARDS = [
    "9225xxxxxxxxxxxx", "9226xxxxxxxxxxxx", "9228xxxxxxxxxxxx", "9200xxxxxxxxxxxx",
    "9235xxxxxxxxxxxx", "9560xxxxxxxxxxxx", "9570xxxxxxxxxxxx"
]

PHONE_PREFIXES = ["+53 5", "5", "+53 6", "6"]
PHONES = [f"{prefix}{random.randint(1000000, 9999999)}" for prefix in PHONE_PREFIXES]

# Dates and Transaction IDs
TX_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
def gen_tx_id():
    return "MM" + "".join(random.choices(TX_CHARS, k=10))

def gen_date():
    day = f"{random.randint(1, 28):02d}"
    month = f"{random.randint(1, 12):02d}"
    year = f"{random.randint(2024, 2026)}"
    return f"{day}/{month}/{year}"

# Let's generate a diverse list of templates
# 1. Gasto (Payments and Outgoing Transfers) - HIGH Confidence
# 2. Ingreso (Received Transfers and Deposits) - HIGH Confidence
# 3. Sin Clasificar / Low/Medium Confidence (General balance alerts, promos, failed/unsuccessful transaction notices, completely random unrelated text, verification OTPs, negations)

dataset = []

# ==============================================================================
# CATEGORY 1: Gastos (Payments / Sent Transfers)
# ==============================================================================
# Gasto CUP
for _ in range(150):
    amount = round(random.uniform(5.0, 5000.0), 2)
    tx = gen_tx_id()
    entity = random.choice(ENTITIES)
    card = random.choice(CARDS)
    phone = random.choice(PHONES)

    templates = [
        f"Pagué utilizando Transfermovil en la Entidad: {entity} Id Compra: {random.randint(1000000,9999999)} Importe: {amount:.2f} CUP Importe pagado: {amount:.2f} CUP No. Transaccion: {tx}",
        f"Factura Pagada: {random.randint(1000000,9999999)} Importe Pagado: {amount:.2f} CUP Importe Factura: {amount:.2f} CUP Nro. Transaccion: {tx}",
        f"Se ha realizado una transferencia a la cuenta {card} de {amount:.2f} CUP. Móvil Origen {phone}.",
        f"Ha realizado un pago de {amount:.2f} CUP a la cuenta {card}.",
        f"EnZona: Pago realizado a {random.choice(COMERCIOS_ENZONA)} por el valor de {amount:.2f} CUP. Transacción {tx}.",
        f"Pago realizado de {amount:.2f} CUP por servicios de electricidad. Transaccion {tx}."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "gasto",
        "monto": amount,
        "moneda": "CUP",
        "confianza": "alta"
    })

# Gasto MLC
for _ in range(50):
    amount = round(random.uniform(1.0, 200.0), 2)
    tx = gen_tx_id()
    card = random.choice(CARDS)

    templates = [
        f"Se ha realizado una transferencia a la cuenta {card} de {amount:.2f} MLC.",
        f"Ha realizado un pago de {amount:.2f} MLC No. Transaccion: {tx}",
        f"Debitado {amount:.2f} MLC para pago en tienda MLC. Nro Transaccion: {tx}"
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "gasto",
        "monto": amount,
        "moneda": "MLC",
        "confianza": "alta"
    })

# Gasto USD
for _ in range(40):
    amount = round(random.uniform(1.0, 100.0), 2)
    tx = gen_tx_id()
    card = random.choice(CARDS)

    templates = [
        f"Se ha realizado una transferencia a la cuenta {card} de {amount:.2f} USD.",
        f"Pago realizado de {amount:.2f} USD a la bolsa MiTransfer. Transaccion {tx}",
        f"Debitado {amount:.2f} USD para pago internacional."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "gasto",
        "monto": amount,
        "moneda": "USD",
        "confianza": "alta"
    })


# ==============================================================================
# CATEGORY 2: Ingresos (Received Transfers / Deposits)
# ==============================================================================
# Ingreso CUP
for _ in range(150):
    amount = round(random.uniform(10.0, 10000.0), 2)
    tx = gen_tx_id()
    card = random.choice(CARDS)
    name = random.choice(NAMES)

    templates = [
        f"Usted ha recibido una transferencia de {amount:.2f} CUP de la cuenta {card}.",
        f"Ha recibido una transferencia de {amount:.2f} CUP de la tarjeta {card}",
        f"EnZona: Se ha recibido una transferencia de {amount:.2f} CUP de la cuenta {card}. Transacción {tx}.",
        f"EnZona: Transferencia recibida por {amount:.2f} CUP de {name}. Transacción: {tx}.",
        f"Acreditado {amount:.2f} CUP de deposito en su cuenta de Transfermovil.",
        f"Su cuenta {card} ha sido acreditada con {amount:.2f} CUP de la tarjeta {card}."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "ingreso",
        "monto": amount,
        "moneda": "CUP",
        "confianza": "alta"
    })

# Ingreso MLC
for _ in range(50):
    amount = round(random.uniform(5.0, 500.0), 2)
    card = random.choice(CARDS)

    templates = [
        f"Se ha recibido un deposito de {amount:.2f} MLC en su tarjeta {card}.",
        f"Usted ha recibido una transferencia de {amount:.2f} MLC de la cuenta {card}.",
        f"Ha recibido una transferencia de {amount:.2f} MLC de la tarjeta {card}"
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "ingreso",
        "monto": amount,
        "moneda": "MLC",
        "confianza": "alta"
    })

# Ingreso USD
for _ in range(40):
    amount = round(random.uniform(5.0, 200.0), 2)
    card = random.choice(CARDS)

    templates = [
        f"Se ha recibido un deposito de {amount:.2f} USD en su monedero USD.",
        f"Usted ha recibido una transferencia de {amount:.2f} USD de la cuenta {card}.",
        f"Bolsa MiTransfer: Depósito recibido de {amount:.2f} USD por recarga internacional."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "ingreso",
        "monto": amount,
        "moneda": "USD",
        "confianza": "alta"
    })


# ==============================================================================
# CATEGORY 3: Sin Clasificar / Low Confidence / Cubacel Balance & Data & Promos
# ==============================================================================
# Cubacel Balance Alerts
for _ in range(70):
    amount = round(random.uniform(10.0, 1500.0), 2)
    date = gen_date()
    templates = [
        f"ETECSA informa: Su saldo principal es de {amount:.2f} CUP, valido hasta {date}.",
        f"ETECSA: Su saldo es {amount:.2f} CUP, expira el {date}.",
        f"Su saldo principal actual es de {amount:.2f} CUP. Vence el {date}."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "sin_clasificar",
        "monto": amount,
        "moneda": "CUP",
        "confianza": "alta"
    })

# Cubacel Data Packages (National vs. International)
for _ in range(70):
    gb_intl = round(random.uniform(1.0, 15.0), 1)
    mb_nac = random.choice([100, 200, 300, 500, 1000, 1500, 2048])
    date = gen_date()
    # Vary order of national vs international
    templates = [
        f"Su paquete de datos tiene {gb_intl} GB de navegacion internacional LTE y {mb_nac} MB de navegacion nacional, validos hasta {date}.",
        f"Usted tiene {mb_nac} MB de bono para navegacion nacional y {gb_intl} GB de datos internacionales, vencen el {date}.",
        f"Recurso Datos: {gb_intl} GB de internet internacional y {mb_nac} MB nacional de bono. Expira: {date}."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "sin_clasificar",
        "monto": gb_intl,  # We can set monto to international GBs or 0.0, let's treat as balance
        "moneda": "CUP",
        "confianza": "alta"
    })

# Cubacel Promos
for _ in range(50):
    gb = random.choice([10, 15, 20, 25, 30])
    date_start = gen_date()
    date_end = gen_date()
    templates = [
        f"ETECSA Promocion Internacional: Del {date_start} al {date_end}, si recargas desde el exterior recibes {gb} GB + Datos ilimitados de 12 a 7 am!",
        f"ETECSA: Nueva promocion, recargue del {date_start} al {date_end} y reciba un bono de 1000.00 CUP de saldo.",
        f"Promoción de Recarga Internacional Cubacel: Recibe {gb} GB de datos internacionales LTE válidos por 30 días."
    ]
    body = random.choice(templates)
    dataset.append({
        "body": body,
        "tipo": "sin_clasificar",
        "monto": 0.0,
        "moneda": "CUP",
        "confianza": "alta"
    })

# Negations / Unsuccessful transactions (gasto or ingreso failed, confidence should be "baja" or type "sin_clasificar")
for _ in range(50):
    amount = round(random.uniform(5.0, 5000.0), 2)
    templates = [
        f"Transferencia fallida: No se pudo realizar la transferencia de {amount:.2f} CUP por saldo insuficiente.",
        f"Operacion cancelada. Su saldo de {amount:.2f} CUP es menor que el importe del pago solicitado.",
        f"ERROR: Pago denegado para la compra de {amount:.2f} CUP en EnZona.",
        f"No se ha podido procesar el deposito de {amount:.2f} CUP. Contacte con el banco.",
        f"Usted intentó transferir {amount:.2f} CUP pero la operacion fue declinada."
    ]
    body = random.choice(templates)
    # Negations are either "sin_clasificar" or marked with "baja" confidence!
    dataset.append({
        "body": body,
        "tipo": "sin_clasificar",
        "monto": amount,
        "moneda": "CUP",
        "confianza": "baja"
    })

# Totally Unrelated messages (OTPs, chat messages, low confidence)
OTP_TEMPLATES = [
    "Su codigo de verificacion para la app de finanzas es [CODE]. No lo comparta.",
    "Hola como estas, nos vemos mas tarde en el parque?",
    "Le recordamos que su cita con el dentista es mañana a las 10:00 AM.",
    "Felicidades! Has ganado una recarga gratis. Registrate aqui: http://spam.cu",
    "Tu codigo de seguridad de EnZona es [CODE]. Expiracion 2 minutos."
]
for _ in range(60):
    code = random.randint(100000, 999999)
    body = random.choice(OTP_TEMPLATES).replace("[CODE]", str(code))
    dataset.append({
        "body": body,
        "tipo": "sin_clasificar",
        "monto": 0.0,
        "moneda": "CUP",
        "confianza": "baja"
    })

# Let's save the generated dataset
print(f"Total dataset size generated: {len(dataset)}")
with open("ml/dataset.json", "w", encoding="utf-8") as f:
    json.dump(dataset, f, ensure_ascii=False, indent=2)

print("Dataset generated successfully in ml/dataset.json.")

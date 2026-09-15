[CUSTOMER]
│
▼  POST /purchase (qty ≤ 3)
[Purchase Controller]
│
├── 1. Circuit Breaker OPEN? ─────────► Return "Sale Paused" (distinct from sold out)
│
└── 2. Run Lua Script on Redis (Key: flashsale:{saleId}:stock)
│
├── Stock == nil (-3) ───────► Return "Not Active / Ended"
├── Stock < Qty  (-1) ───────► Return "Sold Out" (p99 < 500ms, 0 MySQL load)
│
└── Stock ≥ Qty  (≥ 0)
│ (DECRBY stock, init claim state: PROCESSING)
▼
[Push to Redis Stream]
│
▼
Return HTTP: { claimId, status: "PROCESSING" }
│
(Client polls/listens to claimId)
│
═════════════════════╪═════════════════════════════════════════════════════════
ASYNC WORKER      │ (Processes Redis Stream in background)
═════════════════════╪═════════════════════════════════════════════════════════
▼
[Stream Consumer]
│
├── Check Idempotency (Payment.transactionId = claimId)
├── Query MySQL: Saved Address exists?
└── Check Wallet: Sufficient Coin Balance?
│
├── FAIL (No address OR insufficient coins)
│     ├── 1. Release stock: Redis Lua INCRBY (+qty)
│     └── 2. Set claim status: FAILED (No MySQL Order created)
│
└── SUCCESS
├── 1. MySQL Transaction:
│        - Insert Order & OrderItem (salePrice + 30k fee)
│        - Debit Coins from Wallet
│        - Insert Payment ledger row
└── 2. Set claim status: PAID
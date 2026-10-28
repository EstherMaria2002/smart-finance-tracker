-- V1: esquema inicial de Smart Finance Tracker.
-- Todas las columnas de fecha/hora usan TIMESTAMPTZ (nunca TIMESTAMP "a secas").

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- user_id NULL  => categoría del sistema (is_system = true), visible para todos.
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    color      VARCHAR(7),                            -- #RRGGBB
    icon       VARCHAR(40),
    user_id    BIGINT REFERENCES users(id) ON DELETE CASCADE,
    is_system  BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT ck_categories_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_categories_system CHECK ((is_system = true AND user_id IS NULL)
                                        OR (is_system = false AND user_id IS NOT NULL))
);

-- UNIQUE (user_id, name) no protege a las categorías del sistema porque
-- en PostgreSQL NULL != NULL. Este índice parcial cubre ese hueco.
CREATE UNIQUE INDEX uq_categories_system_name
    ON categories (lower(name)) WHERE user_id IS NULL;

CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id      BIGINT        REFERENCES categories(id),
    -- Convención: amount siempre POSITIVO; el signo lo determina "type".
    amount           NUMERIC(14,2) NOT NULL,
    type             VARCHAR(10)   NOT NULL,           -- INCOME | EXPENSE
    description      VARCHAR(255)  NOT NULL,
    occurred_on      DATE          NOT NULL,
    ml_confidence    NUMERIC(4,3),                     -- 0.000 - 1.000 (solo si source = ML)
    category_source  VARCHAR(10),                      -- ML | MANUAL | RULE
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_tx_amount     CHECK (amount > 0),
    CONSTRAINT ck_tx_type       CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_tx_source     CHECK (category_source IS NULL OR category_source IN ('ML', 'MANUAL', 'RULE')),
    CONSTRAINT ck_tx_confidence CHECK (ml_confidence IS NULL OR (ml_confidence >= 0 AND ml_confidence <= 1))
);

CREATE INDEX idx_tx_user_date     ON transactions (user_id, occurred_on DESC);
CREATE INDEX idx_tx_user_category ON transactions (user_id, category_id);

CREATE TABLE budgets (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id   BIGINT        NOT NULL REFERENCES categories(id),
    month         DATE          NOT NULL,              -- siempre el primer día del mes
    limit_amount  NUMERIC(14,2) NOT NULL,
    CONSTRAINT uq_budgets_user_cat_month UNIQUE (user_id, category_id, month),
    CONSTRAINT ck_budgets_month_first_day CHECK (EXTRACT(DAY FROM month) = 1),
    CONSTRAINT ck_budgets_limit CHECK (limit_amount >= 0)
);

CREATE INDEX idx_budgets_user_month ON budgets (user_id, month);

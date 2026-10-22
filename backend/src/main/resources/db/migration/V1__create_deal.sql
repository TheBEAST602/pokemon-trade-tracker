CREATE TABLE deal (
    id            UUID PRIMARY KEY,
    friend_name   VARCHAR(100) NOT NULL,
    trainer_code  VARCHAR(20),
    pokemon_give  VARCHAR(100) NOT NULL,
    pokemon_get   VARCHAR(100) NOT NULL,
    trade_type    VARCHAR(20)  NOT NULL,
    plan_days     INTEGER      NOT NULL DEFAULT 0 CHECK (plan_days >= 0),
    deal_date     DATE         NOT NULL,
    due_date      DATE         NOT NULL,
    priority      VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status        VARCHAR(12)  NOT NULL DEFAULT 'PLANNED',
    special_trade BOOLEAN      NOT NULL DEFAULT FALSE,
    platform      VARCHAR(30),
    chat_link     VARCHAR(500),
    notes         TEXT,
    completed_on  DATE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_deal_status_due ON deal (status, due_date);
CREATE INDEX idx_deal_priority   ON deal (priority);

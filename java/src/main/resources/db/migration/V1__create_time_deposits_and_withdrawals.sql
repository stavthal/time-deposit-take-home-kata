-- Names are quoted so Postgres keeps the camelCase names from the README.
CREATE TABLE "timeDeposits" (
    "id"       INTEGER        PRIMARY KEY,
    "planType" VARCHAR(255)   NOT NULL,
    "days"     INTEGER        NOT NULL,
    "balance"  DECIMAL(19, 2) NOT NULL
);

CREATE TABLE "withdrawals" (
    "id"            INTEGER        PRIMARY KEY,
    "timeDepositId" INTEGER        NOT NULL REFERENCES "timeDeposits" ("id"),
    "amount"        DECIMAL(19, 2) NOT NULL,
    "date"          DATE           NOT NULL
);

CREATE INDEX "idx_withdrawals_timeDepositId" ON "withdrawals" ("timeDepositId");

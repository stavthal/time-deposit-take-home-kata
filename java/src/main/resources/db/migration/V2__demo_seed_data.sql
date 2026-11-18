-- Assumption: demo rows live in a migration so a reviewer can try both endpoints at once.
-- One deposit per rule boundary, plus the README test data (id 7).
INSERT INTO "timeDeposits" ("id", "planType", "days", "balance") VALUES
    (1, 'basic',     30,    1000.00),
    (2, 'basic',     31,    1000.00),
    (3, 'student',  365,    1000.00),
    (4, 'student',  366,    1000.00),
    (5, 'premium',   45,    1000.00),
    (6, 'premium',   46,    1000.00),
    (7, 'basic',     45, 1234567.00);

INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date") VALUES
    (1, 2,   100.00, DATE '2026-09-15'),
    (2, 2,    50.25, DATE '2026-10-01'),
    (3, 3,    20.00, DATE '2026-10-20'),
    (4, 7, 10000.00, DATE '2026-11-02');

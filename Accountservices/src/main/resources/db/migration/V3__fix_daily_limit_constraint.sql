-- V3: Relax chk_daily_limit_positive to allow 0 for FIXED_DEPOSIT and LOAN accounts.
--
-- Root cause: FIXED_DEPOSIT and LOAN accounts intentionally use dailyTxnLimit = 0
-- (no daily debit transactions allowed), but the V2 constraint used `> 0` instead of `>= 0`,
-- causing a constraint violation on every FD / LOAN account creation.

ALTER TABLE accounts
    DROP CHECK chk_daily_limit_positive;

ALTER TABLE accounts
    ADD CONSTRAINT chk_daily_limit_non_negative
        CHECK (daily_txn_limit >= 0);


alter table bank_account
    drop constraint if exists bank_account_balance_non_negative;

create table overdraft_interest_accrual (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    interest_date date not null,
    amount numeric(19, 2) not null,
    constraint uq_overdraft_interest_account_date unique (account_id, interest_date)
);

create index idx_overdraft_interest_account_date
    on overdraft_interest_accrual(account_id, interest_date desc);

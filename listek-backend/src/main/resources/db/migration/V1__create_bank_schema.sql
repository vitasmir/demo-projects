create table bank_account (
    id uuid primary key,
    owner_name varchar(120) not null,
    account_number varchar(34) not null unique,
    balance numeric(19, 2) not null default 0,
    currency varchar(3) not null,
    constraint bank_account_balance_non_negative check (balance >= 0)
);

create table bank_transaction (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    amount numeric(19, 2) not null,
    type varchar(20) not null,
    description varchar(120) not null,
    created_at timestamp with time zone not null
);

create index idx_bank_transaction_account_created
    on bank_transaction(account_id, created_at desc);

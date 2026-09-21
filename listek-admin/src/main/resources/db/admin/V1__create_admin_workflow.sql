alter table loan_application
    add column if not exists decided_at timestamp with time zone,
    add column if not exists decision_note varchar(500);

create table if not exists overdraft_application (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    requested_limit numeric(19, 2) not null check (requested_limit between 1000 and 250000),
    monthly_income numeric(19, 2) not null check (monthly_income >= 0),
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    decided_at timestamp with time zone,
    decision_note varchar(500)
);

create index if not exists idx_overdraft_application_status_created
    on overdraft_application(status, created_at desc);
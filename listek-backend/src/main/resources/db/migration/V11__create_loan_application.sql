create table loan_application (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    type varchar(20) not null,
    amount numeric(19, 2) not null check (amount >= 20000),
    repayment_months integer not null check (repayment_months between 12 and 120),
    annual_rate numeric(7, 4) not null,
    monthly_payment numeric(19, 2) not null,
    purpose varchar(80) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create index idx_loan_application_account_created
    on loan_application(account_id, created_at desc);
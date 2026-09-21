create table standing_order (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    target_account_number varchar(34) not null,
    amount numeric(19, 2) not null,
    description varchar(120) not null,
    day_of_month integer not null,
    active boolean not null default true,
    created_at timestamp with time zone not null
);

create table payment_template (
    id uuid primary key,
    account_id uuid not null references bank_account(id),
    name varchar(80) not null,
    target_account_number varchar(34) not null,
    amount numeric(19, 2) not null,
    description varchar(120) not null,
    created_at timestamp with time zone not null
);
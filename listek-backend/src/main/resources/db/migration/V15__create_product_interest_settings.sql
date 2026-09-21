create table if not exists product_interest_settings (
    id boolean primary key default true check (id),
    savings_rate numeric(6, 3) not null,
    overdraft_rate numeric(6, 3) not null,
    personal_loan_rate numeric(6, 3) not null,
    home_loan_rate numeric(6, 3) not null
);


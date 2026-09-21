alter table product_interest_settings
    add column if not exists mortgage_rate numeric(6, 3) not null default 4.200,
    add column if not exists mortgage_minimum_equity_percent numeric(5, 2) not null default 20.00;
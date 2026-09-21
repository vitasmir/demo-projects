create table if not exists product_interest_settings (
    id boolean primary key default true check (id),
    savings_rate numeric(6, 3) not null,
    overdraft_rate numeric(6, 3) not null,
    personal_loan_rate numeric(6, 3) not null,
    home_loan_rate numeric(6, 3) not null
);

insert into product_interest_settings (id, savings_rate, overdraft_rate, personal_loan_rate, home_loan_rate)
values (true, 4.200, 12.900, 6.900, 5.400)
on conflict (id) do nothing;

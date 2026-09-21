alter table bank_card
    add column if not exists online_payment_limit numeric(19, 2) not null default 30000.00;

alter table bank_card
    add constraint bank_card_online_payment_limit_check check (online_payment_limit >= 0);
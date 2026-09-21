alter table bank_card
    add column if not exists in_store_payments boolean not null default true;
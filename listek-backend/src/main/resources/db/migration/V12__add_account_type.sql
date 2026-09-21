alter table bank_account
    add column account_type varchar(10) not null default 'CURRENT';

alter table bank_account
    add constraint bank_account_account_type_check
    check (account_type in ('CURRENT', 'SAVINGS'));

create unique index ux_bank_account_one_savings_per_owner
    on bank_account (email, account_type)
    where account_type = 'SAVINGS';

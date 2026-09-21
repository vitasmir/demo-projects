alter table bank_account add column username varchar(80);
alter table bank_account add column first_name varchar(80);
alter table bank_account add column last_name varchar(80);
alter table bank_account add column birth_number varchar(11);

create unique index uq_bank_account_current_username_lower
    on bank_account (lower(username))
    where account_type = 'CURRENT';
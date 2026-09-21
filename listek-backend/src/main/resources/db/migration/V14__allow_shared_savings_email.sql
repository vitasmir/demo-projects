drop index if exists uq_bank_account_email_lower;

create unique index uq_bank_account_current_email_lower
    on bank_account (lower(email))
    where account_type = 'CURRENT';

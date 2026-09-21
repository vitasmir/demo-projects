alter table overdraft_application
    add column if not exists annual_rate numeric(7, 4);

create unique index if not exists uq_overdraft_application_account
    on overdraft_application(account_id);
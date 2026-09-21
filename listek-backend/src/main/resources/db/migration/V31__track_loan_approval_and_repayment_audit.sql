alter table loan_application
    add column if not exists approved_by varchar(80),
    add column if not exists approved_at timestamp with time zone,
    add column if not exists repaid_at timestamp with time zone;

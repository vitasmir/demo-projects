alter table loan_application
    add column if not exists repayment_account_number varchar(34),
    add column if not exists variable_symbol varchar(10),
    add column if not exists specific_symbol varchar(10),
    add column if not exists repayment_day_of_month integer,
    add column if not exists repaid_amount numeric(19, 2) not null default 0,
    add column if not exists remaining_installments integer,
    add column if not exists due_date date;

update loan_application
set remaining_installments = repayment_months
where remaining_installments is null;

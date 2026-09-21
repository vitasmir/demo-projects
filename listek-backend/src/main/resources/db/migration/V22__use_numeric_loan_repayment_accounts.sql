update loan_application
set repayment_account_number = '9' || lpad(
    (abs(hashtextextended(id::text, 0)::numeric) % 1000000000000000)::text,
    15,
    '0'
)
where repayment_account_number like 'L-%';

create unique index if not exists uq_loan_repayment_account_number
    on loan_application(repayment_account_number)
    where repayment_account_number is not null;
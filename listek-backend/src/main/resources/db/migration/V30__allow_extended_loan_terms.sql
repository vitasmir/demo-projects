alter table loan_application
    drop constraint if exists loan_application_repayment_months_check;

alter table loan_application
    add constraint loan_application_repayment_months_check
    check (repayment_months between 12 and 360);

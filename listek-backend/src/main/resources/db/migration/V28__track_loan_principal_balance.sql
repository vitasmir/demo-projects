alter table loan_application add column if not exists principal_balance numeric(19, 2);

update loan_application
set principal_balance = greatest(
        amount * power(1 + annual_rate / 1200, floor(repaid_amount / monthly_payment))
        - monthly_payment * (power(1 + annual_rate / 1200, floor(repaid_amount / monthly_payment)) - 1)
          / (annual_rate / 1200),
        0
    )
where principal_balance is null;

update loan_application
set principal_balance = amount
where principal_balance is null;

alter table loan_application alter column principal_balance set not null;
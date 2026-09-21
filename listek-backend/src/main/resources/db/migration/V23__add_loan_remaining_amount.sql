alter table loan_application
    add column if not exists remaining_amount numeric(19, 2) not null default 0;

update loan_application loan
set repaid_amount = coalesce((
        select sum(abs(transaction.amount))
        from bank_transaction transaction
        where transaction.account_id = loan.account_id
          and transaction.type = 'DEBIT'
          and transaction.counterparty_account_number = loan.repayment_account_number
          and transaction.variable_symbol = loan.variable_symbol
          and transaction.specific_symbol = loan.specific_symbol
    ), 0),
    remaining_amount = greatest(
        loan.monthly_payment * loan.repayment_months - coalesce((
            select sum(abs(transaction.amount))
            from bank_transaction transaction
            where transaction.account_id = loan.account_id
              and transaction.type = 'DEBIT'
              and transaction.counterparty_account_number = loan.repayment_account_number
              and transaction.variable_symbol = loan.variable_symbol
              and transaction.specific_symbol = loan.specific_symbol
        ), 0),
        0
    );
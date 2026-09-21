update standing_order standing_order
set target_account_number = loan.repayment_account_number,
    variable_symbol = loan.variable_symbol,
    specific_symbol = loan.specific_symbol
from loan_application loan
where standing_order.description = 'Splátka půjčky'
  and standing_order.target_account_number = 'L-' || upper(replace(loan.id::text, '-', ''))
  and loan.repayment_account_number is not null
  and loan.variable_symbol is not null
  and loan.specific_symbol is not null;

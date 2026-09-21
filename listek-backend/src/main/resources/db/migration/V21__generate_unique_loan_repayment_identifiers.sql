update loan_application
set repayment_account_number = 'L-' || upper(replace(id::text, '-', '')),
    variable_symbol = lpad((abs(hashtext(id::text))::bigint % 10000000000)::text, 10, '0'),
    specific_symbol = lpad((abs(hashtext(id::text || ':specific'))::bigint % 10000000000)::text, 10, '0')
where status = 'APPROVED'
  and (repayment_account_number is null or repayment_account_number = 'LOAN-REPAYMENT');
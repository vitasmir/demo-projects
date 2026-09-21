with legacy_orders as (
    select id, account_id, amount, created_at,
           row_number() over (partition by account_id order by created_at, id) as sequence_number
    from standing_order
    where description = 'Splátka půjčky'
      and target_account_number = 'LOAN-REPAYMENT'
      and coalesce(variable_symbol, '') in ('', '0')
      and coalesce(specific_symbol, '') in ('', '0')
),
loan_candidates as (
    select loan.id, loan.account_id, loan.monthly_payment, loan.repayment_account_number,
           loan.variable_symbol, loan.specific_symbol, loan.created_at,
           row_number() over (partition by loan.account_id order by loan.created_at, loan.id) as sequence_number
    from loan_application loan
    where loan.status = 'APPROVED'
      and loan.repayment_account_number is not null
      and loan.variable_symbol is not null
      and loan.specific_symbol is not null
      and not exists (
          select 1
          from standing_order existing_order
          where existing_order.account_id = loan.account_id
            and existing_order.description = 'Splátka půjčky'
            and existing_order.target_account_number = loan.repayment_account_number
            and existing_order.variable_symbol = loan.variable_symbol
            and existing_order.specific_symbol = loan.specific_symbol
      )
)
update standing_order legacy_order
set target_account_number = loan.repayment_account_number,
    variable_symbol = loan.variable_symbol,
    specific_symbol = loan.specific_symbol
from legacy_orders legacy
join loan_candidates loan
  on loan.account_id = legacy.account_id
 and loan.sequence_number = legacy.sequence_number
 and loan.monthly_payment = legacy.amount
 and loan.created_at <= legacy.created_at
where legacy_order.id = legacy.id;

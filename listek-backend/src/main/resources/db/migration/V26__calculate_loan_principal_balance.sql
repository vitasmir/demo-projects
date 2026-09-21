update loan_application
set remaining_amount = case
        when repaid_amount >= monthly_payment * repayment_months then 0
        else greatest(
            amount * power(1 + annual_rate / 1200, floor(repaid_amount / monthly_payment))
            - monthly_payment * (power(1 + annual_rate / 1200, floor(repaid_amount / monthly_payment)) - 1)
              / (annual_rate / 1200),
            0
        )
    end,
    remaining_installments = case
        when repaid_amount >= monthly_payment * repayment_months then 0
        else repayment_months - floor(repaid_amount / monthly_payment)::int
    end
where status = 'APPROVED';

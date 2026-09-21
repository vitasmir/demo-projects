update loan_application
set remaining_amount = greatest(monthly_payment * repayment_months - repaid_amount, 0),
    remaining_installments = case
        when monthly_payment * repayment_months - repaid_amount <= 0 then 0
        else repayment_months - floor(repaid_amount / monthly_payment)::int
    end
where status = 'APPROVED';
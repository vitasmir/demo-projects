alter table bank_transaction
    add column if not exists variable_symbol varchar(10),
    add column if not exists specific_symbol varchar(10);
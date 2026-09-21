alter table standing_order
    add column if not exists variable_symbol varchar(10),
    add column if not exists specific_symbol varchar(10);

alter table payment_template
    add column if not exists variable_symbol varchar(10),
    add column if not exists specific_symbol varchar(10);
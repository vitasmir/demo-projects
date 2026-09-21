alter table admin_user
    add column if not exists first_name varchar(100),
    add column if not exists last_name varchar(100),
    add column if not exists birth_number varchar(10),
    add column if not exists email varchar(160),
    add column if not exists street varchar(160),
    add column if not exists city varchar(100),
    add column if not exists postal_code varchar(6);
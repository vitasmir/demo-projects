create table standing_order_execution (
    id uuid primary key,
    standing_order_id uuid not null references standing_order(id) on delete cascade,
    execution_month varchar(7) not null,
    constraint uq_standing_order_execution_month unique (standing_order_id, execution_month)
);

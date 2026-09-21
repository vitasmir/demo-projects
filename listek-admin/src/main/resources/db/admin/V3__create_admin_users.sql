create table if not exists admin_user (
    username varchar(80) primary key,
    password_hash varchar(64) not null,
    must_change_password boolean not null default true
);

insert into admin_user (username, password_hash, must_change_password)
values ('admin', '601411a33347f411303e7a78191c7506c40ef05c12c41f0d27ae514441218767', true)
on conflict (username) do nothing;
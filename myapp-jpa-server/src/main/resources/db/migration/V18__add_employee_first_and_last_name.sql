ALTER TABLE employees
    ADD COLUMN first_name character varying(255),
    ADD COLUMN last_name character varying(255);

UPDATE employees
SET first_name = NULLIF(split_part(trim(name), ' ', 1), ''),
    last_name = NULLIF(regexp_replace(trim(name), '^\\S+\\s*', ''), '');
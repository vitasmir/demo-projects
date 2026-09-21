-- FUNCTION: public.insert_dummy_employees()

-- DROP FUNCTION IF EXISTS public.insert_dummy_employees();

CREATE OR REPLACE FUNCTION public.insert_dummy_employees(
	)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
DECLARE
    i INT;
    manager_ids VARCHAR[] := ARRAY[]::VARCHAR[]; -- Initialize the array
    new_employee_id VARCHAR;
    random_manager_id VARCHAR;
    random_first_name VARCHAR;
    random_last_name VARCHAR;
    is_manager BOOLEAN;
    offices VARCHAR[] := ARRAY['New York', 'London', 'Tokyo', 'Paris', 'Sydney'];
    positions VARCHAR[] := ARRAY['Software Engineer', 'Project Manager', 'Sales Associate', 'HR Specialist', 'Accountant', 'Designer'];
    first_names VARCHAR[] := ARRAY['Jan', 'Petr', 'Martin', 'Tomáš', 'Lukáš', 'David', 'Anna', 'Jana', 'Lucie', 'Eva', 'Marie', 'Kateřina'];
    last_names VARCHAR[] := ARRAY['Novák', 'Svoboda', 'Dvořák', 'Černý', 'Procházka', 'Kučera', 'Veselý', 'Horák', 'Němec', 'Pokorný', 'Král', 'Pospíšil'];
BEGIN
    -- First, create 100 top-level managers
    FOR i IN 1..10 LOOP
        new_employee_id := gen_random_uuid()::varchar;
        random_first_name := first_names[1 + floor(random() * array_length(first_names, 1))];
        random_last_name := last_names[1 + floor(random() * array_length(last_names, 1))];
        INSERT INTO employees (id, name, first_name, last_name, position, salary, start_date, office, extn, has_manager_rights, manager_id)
        VALUES (
            new_employee_id,
            random_first_name || ' ' || random_last_name || ' ' || i,
            random_first_name,
            random_last_name,
            'Project Manager',
            (80000 + floor(random() * 70000))::text,
            current_date - (floor(random() * 3650) || ' days')::interval,
            offices[1 + floor(random() * array_length(offices, 1))],
            floor(random() * 9000 + 1000)::text,
            true,
            NULL
        );
        -- Add the new manager to the pool of available managers
        manager_ids := array_append(manager_ids, new_employee_id);
    END LOOP;

    -- Then, create the remaining 14,900 employees
    FOR i IN 11..500 LOOP
        -- Pick a random manager from the existing pool
        random_manager_id := manager_ids[1 + floor(random() * array_length(manager_ids, 1))];
        new_employee_id := gen_random_uuid()::varchar;
        is_manager := (random() > 0.8); -- 20% chance of being a manager
        random_first_name := first_names[1 + floor(random() * array_length(first_names, 1))];
        random_last_name := last_names[1 + floor(random() * array_length(last_names, 1))];

        INSERT INTO employees (id, name, first_name, last_name, position, salary, start_date, office, extn, has_manager_rights, manager_id)
        VALUES (
            new_employee_id,
            random_first_name || ' ' || random_last_name || ' ' || i,
            random_first_name,
            random_last_name,
            positions[1 + floor(random() * array_length(positions, 1))],
            (40000 + floor(random() * 60000))::text,
            current_date - (floor(random() * 3650) || ' days')::interval,
            offices[1 + floor(random() * array_length(offices, 1))],
            floor(random() * 9000 + 1000)::text,
            is_manager,
            random_manager_id
        );

        -- If the new employee is a manager, add them to the pool for future assignments
        IF is_manager THEN
            manager_ids := array_append(manager_ids, new_employee_id);
        END IF;
    END LOOP;
END;
$BODY$; 

ALTER FUNCTION public.insert_dummy_employees()
    OWNER TO postgres;

select public.insert_dummy_employees();
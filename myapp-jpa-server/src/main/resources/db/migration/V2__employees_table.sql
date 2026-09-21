CREATE TABLE employees (
    has_manager_rights boolean NOT NULL,
    start_date date,
    extn character varying(255),
    id character varying(255) NOT NULL,
    manager_id character varying(255),
    name character varying(255),
    office character varying(255),
    position character varying(255),
    salary character varying(255)
);

ALTER TABLE ONLY employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);

ALTER TABLE ONLY employees
    ADD CONSTRAINT fki4365uo9af35g7jtbc2rteukt FOREIGN KEY (manager_id) REFERENCES public.employees(id);
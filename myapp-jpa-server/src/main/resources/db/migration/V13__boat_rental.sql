CREATE TABLE IF NOT EXISTS boats (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS boat_rental_settings (
    id BIGSERIAL PRIMARY KEY,
    day_start_time TIME NOT NULL,
    day_end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS boat_reservations (
    id BIGSERIAL PRIMARY KEY,
    boat_id BIGINT NOT NULL REFERENCES boats(id) ON DELETE CASCADE,
    reserved_by VARCHAR(255) NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_boat_reservation_boat_start') THEN
        CREATE INDEX idx_boat_reservation_boat_start ON boat_reservations(boat_id, start_date_time);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_boat_reservation_start_end') THEN
        CREATE INDEX idx_boat_reservation_start_end ON boat_reservations(start_date_time, end_date_time);
    END IF;
END$$;

INSERT INTO boats (name)
SELECT v.name FROM (VALUES ('Lodka 1'), ('Lodka 2')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM boats b WHERE b.name = v.name);

INSERT INTO boat_rental_settings (day_start_time, day_end_time, slot_duration_minutes)
SELECT '08:00:00'::time, '18:00:00'::time, 30
WHERE NOT EXISTS (SELECT 1 FROM boat_rental_settings);

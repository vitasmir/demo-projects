ALTER TABLE boat_reservations
ADD COLUMN IF NOT EXISTS reserved_by_user_id VARCHAR(255);

UPDATE boat_reservations
SET reserved_by_user_id = reserved_by
WHERE reserved_by_user_id IS NULL;

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'public'
		  AND table_name = 'boat_reservations'
		  AND column_name = 'reserved_by_user_id'
	) THEN
		ALTER TABLE boat_reservations
		ALTER COLUMN reserved_by_user_id SET NOT NULL;
	END IF;
END $$;

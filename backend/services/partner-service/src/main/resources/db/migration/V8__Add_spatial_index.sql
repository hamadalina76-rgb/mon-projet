-- Partner Service - Add spatial GiST index for nearby partner queries
-- Requires PostGIS (already enabled via V1 migration)
CREATE INDEX IF NOT EXISTS idx_partners_location_gist
    ON partners
    USING GIST (
        CAST(
            ST_MakePoint(
                CAST(longitude AS float8),
                CAST(latitude  AS float8)
            ) AS geography
        )
    )
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

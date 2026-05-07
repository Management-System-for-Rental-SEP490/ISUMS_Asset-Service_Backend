ALTER TABLE asset_event_images
    ADD COLUMN IF NOT EXISTS type VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_asset_event_images_event_type_created_at
    ON asset_event_images (event_id, type, created_at);

-- Phase 5e i18n: per-locale translation map for AssetEvent.note. AssetItem
-- already has note_translations from a prior migration; this fills the gap on
-- AssetEvent rows so action history is readable across locales.
ALTER TABLE "assetEvents"
    ADD COLUMN IF NOT EXISTS note_translations TEXT;

COMMENT ON COLUMN "assetEvents".note_translations IS
    'JSON map of locale -> translated event note. Reserved keys: _source, _auto.';

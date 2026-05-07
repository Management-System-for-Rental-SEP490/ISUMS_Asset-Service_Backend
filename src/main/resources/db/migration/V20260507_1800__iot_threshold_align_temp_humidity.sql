-- Backfill temperature + humidity threshold rows for any existing house
-- that doesn't have them. Match values just deployed to DynamoDB
-- esp32_thresholds (verified via aws dynamodb scan 2026-05-07):
--   temperature: max_val=80, severity=CRITICAL
--   humidity:    max_val=90, severity=WARNING
--
-- Why backfill: new IotThresholdService.seedDefaults values changed from
-- (temp 45 WARNING, humidity 90 INFO) to (temp 80 CRITICAL, humidity 90 WARNING)
-- to match what the Lambda threshold-checker actually fires on.
-- Existing houses created before this realignment would otherwise drift
-- between PG (BE source) and DDB (Lambda source).
--
-- Idempotent: only inserts where no row exists yet for (house_id, area_id IS NULL, metric).

INSERT INTO iot_thresholds (id, house_id, area_id, metric, min_val, max_val, enabled, severity, created_at, updated_at)
SELECT
    gen_random_uuid(),
    h.house_id,
    NULL,
    'temperature',
    NULL,
    80.0,
    true,
    'CRITICAL',
    now(),
    now()
FROM (
    SELECT DISTINCT house_id
    FROM iot_thresholds
    WHERE area_id IS NULL
) h
WHERE NOT EXISTS (
    SELECT 1 FROM iot_thresholds t
    WHERE t.house_id = h.house_id AND t.area_id IS NULL AND t.metric = 'temperature'
);

INSERT INTO iot_thresholds (id, house_id, area_id, metric, min_val, max_val, enabled, severity, created_at, updated_at)
SELECT
    gen_random_uuid(),
    h.house_id,
    NULL,
    'humidity',
    NULL,
    90.0,
    true,
    'WARNING',
    now(),
    now()
FROM (
    SELECT DISTINCT house_id
    FROM iot_thresholds
    WHERE area_id IS NULL
) h
WHERE NOT EXISTS (
    SELECT 1 FROM iot_thresholds t
    WHERE t.house_id = h.house_id AND t.area_id IS NULL AND t.metric = 'humidity'
);

UPDATE iot_thresholds
SET max_val = 80.0,
    severity = 'CRITICAL',
    updated_at = now()
WHERE area_id IS NULL
  AND metric = 'temperature'
  AND (max_val < 80 OR severity != 'CRITICAL');

UPDATE iot_thresholds
SET max_val = 90.0,
    severity = 'WARNING',
    updated_at = now()
WHERE area_id IS NULL
  AND metric = 'humidity'
  AND severity = 'INFO';

-- IoT indoor-safety configuration (versioned, immutable history).
--
-- One row = one published version of the safety config.
-- Active version = WHERE expired_at IS NULL ORDER BY effective_from DESC LIMIT 1.
--
-- Mutation rules:
--   - INSERT new row when admin updates config
--     → previously active row gets expired_at = effective_at_of_new
--   - NEVER UPDATE config_json once a row is published
--   - Seed row uses all-zeros UUID as created_by (system marker)
--
-- Why JSON column instead of normalized tables:
--   - Config has heterogeneous nested arrays (sensors, gaps, thresholds,
--     score components, bands) with different shapes — 5 sub-tables would
--     be over-normalized for a small registry that's read >> written.
--   - PostgreSQL native JSONB enables admin tooling to inspect/diff
--     versions easily. Schema evolution is forward-compatible.

CREATE TABLE IF NOT EXISTS iot_safety_config_version (
    id              uuid        PRIMARY KEY,
    version         varchar(80) NOT NULL,
    config_json     jsonb       NOT NULL,
    effective_from  timestamptz NOT NULL DEFAULT now(),
    expired_at      timestamptz,
    notes           varchar(1000),
    created_by      uuid        NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    expired_by      uuid,

    CONSTRAINT chk_isc_dates CHECK (expired_at IS NULL OR expired_at > effective_from)
);

CREATE INDEX IF NOT EXISTS idx_isc_active
    ON iot_safety_config_version (effective_from DESC)
    WHERE expired_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_isc_history
    ON iot_safety_config_version (effective_from DESC);

INSERT INTO iot_safety_config_version (id, version, config_json, created_by, notes)
VALUES (
    '5a1e7c00-0000-0000-0000-000000000001',
    'iot-safety-2026-05',
    '{
      "activeSensors": [
        {
          "code": "MQ2",
          "displayName": "MQ-2 — Cảm biến gas dễ cháy + khói",
          "model": "MQ-2 (Hanwei Electronics)",
          "measures": ["gas_ppm", "smoke"],
          "accuracyNotes": "Cross-sensitive: phát hiện LPG/methane/propane/khói nhưng không phân biệt được loại gas. Đơn vị ppm là xấp xỉ, chưa calibrated cho từng gas riêng.",
          "calibrationStatus": "factory_default",
          "datasheet": "https://www.pololu.com/file/0J309/MQ2.pdf"
        },
        {
          "code": "DHT22",
          "displayName": "DHT22 — Cảm biến nhiệt độ và độ ẩm",
          "model": "DHT22 / AM2302 (Aosong)",
          "measures": ["temp_c", "humidity_pct"],
          "accuracyNotes": "Sai số ±0.5°C, ±2% RH. Phù hợp cho indoor monitoring, không phù hợp cho ứng dụng laboratory.",
          "calibrationStatus": "factory_default",
          "datasheet": "https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf"
        }
      ],
      "capabilityGaps": [
        {"metric":"pm25","displayName":"PM2.5","description":"Bụi mịn dưới 2.5μm — chỉ số chính của AQI","requiredSensor":"PMS5003 (Plantower) hoặc tương đương","sensorPriceVndApprox":700000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 24h: 50μg/m³)"},
        {"metric":"pm10","displayName":"PM10","description":"Bụi mịn dưới 10μm","requiredSensor":"PMS5003 hoặc SDS011","sensorPriceVndApprox":400000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 24h: 100μg/m³)"},
        {"metric":"co","displayName":"CO","description":"Carbon monoxide — khí CO độc","requiredSensor":"MQ-7 hoặc MH-Z19B (CO + CO2 NDIR)","sensorPriceVndApprox":400000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 1h: 30mg/m³)"},
        {"metric":"co2","displayName":"CO₂","description":"Carbon dioxide — chỉ báo thông gió","requiredSensor":"MH-Z19B / SCD30 / SCD41","sensorPriceVndApprox":500000,"standardRef":"ASHRAE 62.1 (mức an toàn <1000 ppm)"},
        {"metric":"no2","displayName":"NO₂","description":"Nitrogen dioxide","requiredSensor":"MiCS-2714 hoặc Spec NO2","sensorPriceVndApprox":1500000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 1h: 200μg/m³)"},
        {"metric":"voc","displayName":"VOC","description":"Hợp chất hữu cơ dễ bay hơi","requiredSensor":"BME680 hoặc CCS811","sensorPriceVndApprox":250000,"standardRef":"WHO Indoor Air Quality Guidelines"}
      ],
      "thresholds": [
        {"metric":"gas_ppm","displayName":"Khí gas dễ cháy","unit":"ppm","comfortMin":0,"comfortMax":50,"warningMin":50,"warningMax":150,"criticalThreshold":200,"criticalIsHigh":true,"standardRef":"OSHA Combustible Gas Lower Explosion Limit (LEL); ngưỡng cảnh báo cháy nổ thông dụng cho cảm biến MQ-2"},
        {"metric":"temp_c","displayName":"Nhiệt độ","unit":"°C","comfortMin":22,"comfortMax":28,"warningMin":18,"warningMax":32,"criticalThreshold":null,"criticalIsHigh":null,"standardRef":"ASHRAE Standard 55-2020 — Thermal Environmental Conditions for Human Occupancy (climate zone hot-humid 22-28°C)"},
        {"metric":"humidity_pct","displayName":"Độ ẩm","unit":"%","comfortMin":40,"comfortMax":70,"warningMin":30,"warningMax":80,"criticalThreshold":null,"criticalIsHigh":null,"standardRef":"EPA Indoor Air Quality (IAQ) Guidelines — dải comfort 40-70%, ngoài 30-80% gây khó chịu hoặc nấm mốc"}
      ],
      "scoreComponents": [
        {"metric":"gas_ppm","weight":0.50,"normalizationStrategy":"ratioToCritical","normalizationParam":300},
        {"metric":"temp_c","weight":0.25,"normalizationStrategy":"absDeviationFromCenter","normalizationParam":25},
        {"metric":"humidity_pct","weight":0.25,"normalizationStrategy":"absDeviationFromCenter","normalizationParam":55}
      ],
      "bands": [
        {"code":"SAFE","label":"An toàn","description":"Không phát hiện rò rỉ gas, nhiệt độ và độ ẩm dễ chịu","scoreMax":30,"severity":"GOOD"},
        {"code":"CAUTION","label":"Cần chú ý","description":"Một số chỉ số ngoài dải khuyến nghị, hãy kiểm tra","scoreMax":60,"severity":"WARNING"},
        {"code":"POOR","label":"Bất ổn","description":"Nên thông gió, kiểm tra bếp gas và điều hòa","scoreMax":85,"severity":"WARNING"},
        {"code":"DANGER","label":"Nguy hiểm","description":"Khả năng cao có rò rỉ gas — mở cửa thông gió, không bật bếp","scoreMax":100,"severity":"CRITICAL"}
      ],
      "disclaimer": "Chỉ số an toàn ở đây là composite weighted score từ MQ-2 + DHT22, ưu tiên phát hiện rò rỉ gas + duy trì comfort. KHÔNG phải AQI theo QCVN 05:2023/BTNMT (yêu cầu PM2.5/PM10/CO/O3/NO2/SO2 đo bằng cảm biến chuyên dụng).",
      "scoreFormulaDescription": "Score = 0.5 × ratioToCritical(gas, 300ppm) + 0.25 × absDeviation(temp, 25°C) × 6 + 0.25 × absDeviation(humidity, 55%) × 2. Tất cả thành phần clamp về 0-100. Score càng thấp càng an toàn.",
      "standardsApplied": "ASHRAE 55-2020 (thermal comfort) · EPA IAQ Guidelines (humidity) · OSHA LEL (combustible gas) · QCVN 05:2023/BTNMT (reference for non-implemented metrics)"
    }'::jsonb,
    '00000000-0000-0000-0000-000000000000',
    'Initial seed: hardware MQ-2 + DHT22, citations cho QCVN 05/ASHRAE 55/EPA IAQ/OSHA LEL.'
)
ON CONFLICT (id) DO NOTHING;

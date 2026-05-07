-- Realign iot_safety_config thresholds to match values actually deployed
-- in DynamoDB esp32_thresholds (the source of truth that the Lambda
-- threshold-checker reads when emitting alerts).
--
-- Discrepancy found via direct DDB scan on 2026-05-07:
--   gas_ppm    DDB CRITICAL=300  | safety_config CRITICAL=200  → align to 300
--   temperature DDB CRITICAL=80   | safety_config no critical    → align critical=80, warning band 18-38
--   (humidity is not in DDB / not an alarm; kept as comfort hint only)
--
-- Pattern: expire previous version (effective_at_of_new = now), publish
-- new immutable row. Mobile fetches latest within 1h cache.

UPDATE iot_safety_config_version
SET expired_at = now(),
    expired_by = '00000000-0000-0000-0000-000000000000'
WHERE id = '5a1e7c00-0000-0000-0000-000000000001'
  AND expired_at IS NULL;

INSERT INTO iot_safety_config_version (id, version, config_json, created_by, notes)
VALUES (
    '5a1e7c00-0000-0000-0000-000000000002',
    'iot-safety-2026-05-realigned',
    '{
      "activeSensors": [
        {"code":"MQ2","displayName":"MQ-2 — Cảm biến gas dễ cháy + khói","model":"MQ-2 (Hanwei Electronics)","measures":["gas_ppm","smoke"],"accuracyNotes":"Cross-sensitive: phát hiện LPG/methane/propane/khói nhưng không phân biệt được loại gas. Đơn vị ppm là xấp xỉ.","calibrationStatus":"factory_default","datasheet":"https://www.pololu.com/file/0J309/MQ2.pdf"},
        {"code":"DHT22","displayName":"DHT22 — Cảm biến nhiệt độ và độ ẩm","model":"DHT22 / AM2302 (Aosong)","measures":["temp_c","humidity_pct"],"accuracyNotes":"Sai số ±0.5°C, ±2% RH. Phù hợp indoor monitoring.","calibrationStatus":"factory_default","datasheet":"https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf"}
      ],
      "capabilityGaps": [
        {"metric":"pm25","displayName":"PM2.5","description":"Bụi mịn dưới 2.5μm — chỉ số chính của AQI","requiredSensor":"PMS5003 (Plantower) hoặc tương đương","sensorPriceVndApprox":700000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 24h: 50μg/m³)"},
        {"metric":"pm10","displayName":"PM10","description":"Bụi mịn dưới 10μm","requiredSensor":"PMS5003 hoặc SDS011","sensorPriceVndApprox":400000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 24h: 100μg/m³)"},
        {"metric":"co","displayName":"CO","description":"Carbon monoxide — khí CO độc","requiredSensor":"MQ-7 hoặc MH-Z19B","sensorPriceVndApprox":400000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 1h: 30mg/m³)"},
        {"metric":"co2","displayName":"CO₂","description":"Carbon dioxide — chỉ báo thông gió","requiredSensor":"MH-Z19B / SCD30 / SCD41","sensorPriceVndApprox":500000,"standardRef":"ASHRAE 62.1 (mức an toàn <1000 ppm)"},
        {"metric":"no2","displayName":"NO₂","description":"Nitrogen dioxide","requiredSensor":"MiCS-2714 hoặc Spec NO2","sensorPriceVndApprox":1500000,"standardRef":"QCVN 05:2023/BTNMT (giới hạn 1h: 200μg/m³)"},
        {"metric":"voc","displayName":"VOC","description":"Hợp chất hữu cơ dễ bay hơi","requiredSensor":"BME680 hoặc CCS811","sensorPriceVndApprox":250000,"standardRef":"WHO Indoor Air Quality Guidelines"}
      ],
      "thresholds": [
        {"metric":"gas_ppm","displayName":"Khí gas dễ cháy","unit":"ppm","comfortMin":0,"comfortMax":50,"warningMin":50,"warningMax":300,"criticalThreshold":300,"criticalIsHigh":true,"standardRef":"OSHA Combustible Gas LEL · Khớp với esp32_thresholds DynamoDB (Lambda alarm) max_val=300 CRITICAL"},
        {"metric":"temp_c","displayName":"Nhiệt độ","unit":"°C","comfortMin":22,"comfortMax":28,"warningMin":18,"warningMax":38,"criticalThreshold":80,"criticalIsHigh":true,"standardRef":"ASHRAE 55-2020 (comfort 22-28°C) · Lambda fire-risk alarm tại 80°C — khớp esp32_thresholds DynamoDB"},
        {"metric":"humidity_pct","displayName":"Độ ẩm","unit":"%","comfortMin":40,"comfortMax":70,"warningMin":30,"warningMax":90,"criticalThreshold":null,"criticalIsHigh":null,"standardRef":"EPA IAQ Guidelines (comfort 40-70%) · Hiển thị FE; chưa có alarm Lambda cho humidity"}
      ],
      "scoreComponents": [
        {"metric":"gas_ppm","weight":0.50,"normalizationStrategy":"ratioToCritical","normalizationParam":300},
        {"metric":"temp_c","weight":0.25,"normalizationStrategy":"absDeviationFromCenter","normalizationParam":25},
        {"metric":"humidity_pct","weight":0.25,"normalizationStrategy":"absDeviationFromCenter","normalizationParam":55}
      ],
      "bands": [
        {"code":"SAFE","label":"An toàn","description":"Không phát hiện rò rỉ gas, nhiệt độ và độ ẩm dễ chịu","scoreMax":30,"severity":"GOOD"},
        {"code":"CAUTION","label":"Cần chú ý","description":"Một số chỉ số ngoài dải khuyến nghị","scoreMax":60,"severity":"WARNING"},
        {"code":"POOR","label":"Bất ổn","description":"Nên thông gió, kiểm tra bếp gas và điều hòa","scoreMax":85,"severity":"WARNING"},
        {"code":"DANGER","label":"Nguy hiểm","description":"Khả năng cao có rò rỉ gas — mở cửa thông gió, không bật bếp","scoreMax":100,"severity":"CRITICAL"}
      ],
      "disclaimer": "Chỉ số an toàn ở đây là composite weighted score từ MQ-2 + DHT22, ưu tiên phát hiện rò rỉ gas + duy trì comfort. Ngưỡng critical (gas 300ppm, temperature 80°C) khớp đúng với esp32_thresholds DynamoDB — Lambda threshold-checker dùng cùng giá trị để fire push notification + voice call. KHÔNG phải AQI theo QCVN 05:2023/BTNMT.",
      "scoreFormulaDescription": "Score = 0.5 × ratioToCritical(gas, 300ppm) + 0.25 × absDeviation(temp, 25°C) × 6 + 0.25 × absDeviation(humidity, 55%) × 2. Tất cả thành phần clamp 0-100. Score càng thấp càng an toàn.",
      "standardsApplied": "ASHRAE 55-2020 · EPA IAQ Guidelines · OSHA LEL · QCVN 05:2023/BTNMT (reference) · DynamoDB esp32_thresholds (single source of truth cho alarm)"
    }'::jsonb,
    '00000000-0000-0000-0000-000000000000',
    'Realign thresholds với DynamoDB esp32_thresholds (gas 300 critical, temp 80 critical) — verified via aws dynamodb scan 2026-05-07.'
)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS iot_device_capabilities (
    device_id UUID NOT NULL,
    capability VARCHAR(255) NOT NULL,
    CONSTRAINT fk_iot_device_capabilities_device
        FOREIGN KEY (device_id) REFERENCES iot_devices(id) ON DELETE CASCADE,
    CONSTRAINT pk_iot_device_capabilities
        PRIMARY KEY (device_id, capability)
);

CREATE INDEX IF NOT EXISTS idx_iot_device_capabilities_device
    ON iot_device_capabilities (device_id);

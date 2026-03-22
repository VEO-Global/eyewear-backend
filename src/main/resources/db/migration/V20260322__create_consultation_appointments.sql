CREATE TABLE consultation_appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    appointment_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    CONSTRAINT chk_consultation_appointment_status
        CHECK (status IN ('pending', 'completed'))
);

CREATE INDEX idx_consultation_appointments_time
    ON consultation_appointments (appointment_time);

CREATE INDEX idx_consultation_appointments_phone
    ON consultation_appointments (phone_number);

CREATE INDEX idx_consultation_appointments_status
    ON consultation_appointments (status);

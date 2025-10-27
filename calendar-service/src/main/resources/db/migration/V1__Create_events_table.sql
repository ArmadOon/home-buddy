-- V1__Create_events_table.sql
-- Create table for calendar events

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    household_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    assigned_to BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    all_day_event BOOLEAN NOT NULL DEFAULT false,
    location VARCHAR(500),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_recurring BOOLEAN NOT NULL DEFAULT false,
    recurrence_rule TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMP,
    completed_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_events_household_id ON events(household_id);
CREATE INDEX idx_events_household_dates ON events(household_id, start_date_time, end_date_time);
CREATE INDEX idx_events_assigned_to ON events(assigned_to);
CREATE INDEX idx_events_created_by ON events(created_by);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_event_type ON events(event_type);
CREATE INDEX idx_events_start_date_time ON events(start_date_time);

-- Check constraints
ALTER TABLE events ADD CONSTRAINT check_event_dates
    CHECK (start_date_time <= end_date_time);

ALTER TABLE events ADD CONSTRAINT check_event_type
    CHECK (event_type IN ('CHORE', 'APPOINTMENT', 'REMINDER', 'SOCIAL', 'OTHER'));

ALTER TABLE events ADD CONSTRAINT check_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE events ADD CONSTRAINT check_status
    CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'));

-- Comments
COMMENT ON TABLE events IS 'Calendar events for households';
COMMENT ON COLUMN events.household_id IS 'FK reference to household (auth-service)';
COMMENT ON COLUMN events.created_by IS 'FK reference to user who created the event (auth-service)';
COMMENT ON COLUMN events.assigned_to IS 'FK reference to user assigned to this event (auth-service), nullable';
COMMENT ON COLUMN events.recurrence_rule IS 'RFC 5545 recurrence rule or simplified format';
COMMENT ON COLUMN events.completed_at IS 'Timestamp when event was marked as completed';
COMMENT ON COLUMN events.completed_by IS 'User ID who completed the event';

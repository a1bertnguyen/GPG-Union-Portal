CREATE TABLE integration_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    integration_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    successful_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    started_by VARCHAR(80) NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    error_summary VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integration_runs_created_at ON integration_runs(created_at);
CREATE INDEX idx_integration_runs_type_status ON integration_runs(integration_type, status);

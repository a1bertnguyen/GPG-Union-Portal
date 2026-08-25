CREATE TABLE union_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    location VARCHAR(150),
    chairperson VARCHAR(120),
    term_start DATE,
    term_end DATE,
    decision_number VARCHAR(80),
    legal_status VARCHAR(40) NOT NULL,
    contact_person VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(40) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    union_unit_id BIGINT NOT NULL,
    job_title VARCHAR(120),
    workplace VARCHAR(150),
    join_date DATE,
    membership_status VARCHAR(30) NOT NULL,
    employment_status VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE welfare_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_code VARCHAR(40) NOT NULL UNIQUE,
    welfare_type VARCHAR(30) NOT NULL,
    union_unit_id BIGINT NOT NULL,
    beneficiary_name VARCHAR(150) NOT NULL,
    event_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    document_status VARCHAR(30) NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_welfare_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE labor_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_code VARCHAR(40) NOT NULL UNIQUE,
    received_date DATE NOT NULL,
    union_unit_id BIGINT NOT NULL,
    issue_group VARCHAR(120) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    owner_name VARCHAR(150) NOT NULL,
    deadline DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    affected_people INT NOT NULL DEFAULT 1,
    result_text VARCHAR(2000),
    overdue_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_case_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE union_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    union_unit_id BIGINT NOT NULL,
    event_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    objective VARCHAR(1000),
    planned_budget DECIMAL(15,2) NOT NULL DEFAULT 0,
    actual_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
    participant_count INT NOT NULL DEFAULT 0,
    usefulness_score DECIMAL(3,2),
    report_completed BOOLEAN NOT NULL DEFAULT FALSE,
    follow_up_owner VARCHAR(150),
    follow_up_deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE finance_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_code VARCHAR(40) NOT NULL UNIQUE,
    union_unit_id BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    category VARCHAR(120) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    document_number VARCHAR(80),
    document_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_finance_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE monthly_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    report_month DATE NOT NULL,
    prepared_by VARCHAR(150) NOT NULL,
    plan_next_month VARCHAR(2000),
    support_request VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_report_unit_month UNIQUE (union_unit_id, report_month),
    CONSTRAINT fk_report_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE INDEX idx_members_unit ON members(union_unit_id);
CREATE INDEX idx_welfare_event_date ON welfare_records(event_date);
CREATE INDEX idx_cases_deadline_status ON labor_cases(deadline, status);
CREATE INDEX idx_activities_event_date ON union_activities(event_date);
CREATE INDEX idx_finance_transaction_date ON finance_entries(transaction_date);
CREATE INDEX idx_reports_month ON monthly_reports(report_month);

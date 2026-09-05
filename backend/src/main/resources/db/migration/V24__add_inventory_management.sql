CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    item_code VARCHAR(60) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(120) NULL,
    supplier VARCHAR(200) NULL,
    unit_of_measure VARCHAR(40) NOT NULL DEFAULT 'Cái',
    minimum_stock INT NOT NULL DEFAULT 0,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_item_unit_code UNIQUE (union_unit_id, item_code),
    CONSTRAINT fk_inventory_item_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id)
);

CREATE TABLE inventory_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    receipt_date DATE NOT NULL,
    quantity INT NOT NULL,
    supplier VARCHAR(200) NULL,
    reference_no VARCHAR(80) NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_receipt_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT fk_inventory_receipt_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
);

CREATE TABLE inventory_gift_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    quantity INT NOT NULL,
    program_name VARCHAR(200) NULL,
    reference_no VARCHAR(80) NULL,
    note VARCHAR(1000) NULL,
    employee_code_snapshot VARCHAR(40) NOT NULL,
    recipient_name_snapshot VARCHAR(150) NOT NULL,
    company_name_snapshot VARCHAR(150) NOT NULL,
    job_title_snapshot VARCHAR(120) NULL,
    professional_title_snapshot VARCHAR(120) NULL,
    workplace_snapshot VARCHAR(150) NULL,
    email_snapshot VARCHAR(150) NULL,
    phone_snapshot VARCHAR(30) NULL,
    gender_snapshot VARCHAR(10) NULL,
    place_of_birth_snapshot VARCHAR(150) NULL,
    current_residence_snapshot VARCHAR(200) NULL,
    start_work_date_snapshot DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_issue_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT fk_inventory_issue_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_inventory_issue_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE INDEX idx_inventory_items_unit_name ON inventory_items(union_unit_id, item_name);
CREATE INDEX idx_inventory_receipts_unit_date ON inventory_receipts(union_unit_id, receipt_date);
CREATE INDEX idx_inventory_receipts_item ON inventory_receipts(inventory_item_id);
CREATE INDEX idx_inventory_gift_issues_unit_date ON inventory_gift_issues(union_unit_id, issue_date);
CREATE INDEX idx_inventory_gift_issues_item ON inventory_gift_issues(inventory_item_id);
CREATE INDEX idx_inventory_gift_issues_member ON inventory_gift_issues(member_id);

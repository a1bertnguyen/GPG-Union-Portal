-- Dashboard vụ việc lọc theo tháng tiếp nhận; chỉ mục hiện hữu chỉ bao phủ deadline/status.
CREATE INDEX idx_labor_cases_received_date ON labor_cases(received_date);

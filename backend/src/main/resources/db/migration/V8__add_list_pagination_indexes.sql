-- Indexes for the columns the paginated list endpoints now filter and sort on.
-- Before this change every list query loaded the whole table and filtered in memory, so these
-- columns were never used as access paths; with server-side paging they are hit on every request.
--
-- Deliberately omitted because an existing index already covers them:
--   welfare_records(event_date)      -> idx_welfare_event_date (V1)
--   labor_cases(deadline)            -> idx_cases_deadline_status (V1), deadline is the leading column
--   union_activities(event_date)     -> idx_activities_event_date (V1)
--   finance_entries(transaction_date)-> idx_finance_transaction_date (V1)
--   integration_runs(created_at)     -> idx_integration_runs_created_at (V6)
--   pulse_surveys(union_unit_id)     -> idx_pulse_survey_unit_status (V3), unit is the leading column
--   members(union_unit_id)           -> idx_members_unit (V1)
-- The remaining union_unit_id columns are covered by their foreign-key indexes.

-- Default sort order for the member list.
CREATE INDEX idx_members_full_name ON members(full_name);

-- Status filters and the metric card counts, which run one COUNT per card.
CREATE INDEX idx_members_membership_status ON members(membership_status);
CREATE INDEX idx_welfare_records_status ON welfare_records(status);
CREATE INDEX idx_labor_cases_status ON labor_cases(status);
CREATE INDEX idx_union_activities_status ON union_activities(status);
CREATE INDEX idx_finance_entries_entry_type ON finance_entries(entry_type);

-- "Vụ việc lặp lại" runs a correlated COUNT over this column for every candidate row.
CREATE INDEX idx_labor_cases_issue_group ON labor_cases(issue_group);

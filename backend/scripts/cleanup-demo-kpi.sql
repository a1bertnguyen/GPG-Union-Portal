-- Xóa đúng bộ dữ liệu minh họa KPI; không ảnh hưởng CĐCS khác.
START TRANSACTION;
SET @demo_unit_id = (SELECT id FROM union_units WHERE code='DEMO-KPI-2025' LIMIT 1);
DELETE FROM kpi_population_members WHERE snapshot_id IN (SELECT id FROM kpi_population_snapshots WHERE union_unit_id=@demo_unit_id);
DELETE FROM kpi_population_snapshots WHERE union_unit_id=@demo_unit_id;
DELETE FROM member_changes WHERE member_id IN (SELECT id FROM members WHERE union_unit_id=@demo_unit_id);
DELETE FROM welfare_records WHERE union_unit_id=@demo_unit_id;
DELETE FROM labor_cases WHERE union_unit_id=@demo_unit_id;
DELETE FROM union_activities WHERE union_unit_id=@demo_unit_id;
DELETE FROM finance_entries WHERE union_unit_id=@demo_unit_id;
DELETE FROM monthly_reports WHERE union_unit_id=@demo_unit_id;
DELETE FROM members WHERE union_unit_id=@demo_unit_id;
DELETE FROM union_units WHERE id=@demo_unit_id;
COMMIT;

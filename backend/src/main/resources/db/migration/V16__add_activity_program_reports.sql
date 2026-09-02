-- MySQL commits every ALTER TABLE statement.  This helper lets a failed local V16
-- resume without discarding its already-added columns after `flyway repair`.
-- Narrative fields are TEXT so their possible UTF-8 size does not exceed MySQL's
-- 65,535-byte row limit (the former VARCHAR layout failed at output_proposal).
DELIMITER //

CREATE PROCEDURE gpg_v16_add_column_if_missing(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE v_column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_column_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'union_activities'
      AND column_name = p_column_name;

    IF v_column_exists = 0 THEN
        SET @gpg_v16_sql = CONCAT(
            'ALTER TABLE `union_activities` ADD COLUMN `', p_column_name, '` ', p_column_definition
        );
        PREPARE gpg_v16_statement FROM @gpg_v16_sql;
        EXECUTE gpg_v16_statement;
        DEALLOCATE PREPARE gpg_v16_statement;
    END IF;
END //

DELIMITER ;

CALL gpg_v16_add_column_if_missing('event_time', 'TIME');
CALL gpg_v16_add_column_if_missing('location', 'VARCHAR(300)');
CALL gpg_v16_add_column_if_missing('program_pic', 'VARCHAR(150)');
CALL gpg_v16_add_column_if_missing('invited_count', 'INT NOT NULL DEFAULT 0');
CALL gpg_v16_add_column_if_missing('employee_group', 'VARCHAR(500)');
CALL gpg_v16_add_column_if_missing('actual_content', 'TEXT');
CALL gpg_v16_add_column_if_missing('plan_difference', 'TEXT');
CALL gpg_v16_add_column_if_missing('workers_reached', 'INT NOT NULL DEFAULT 0');
CALL gpg_v16_add_column_if_missing('output_proposal', 'TEXT');
CALL gpg_v16_add_column_if_missing('communication_content', 'TEXT');
CALL gpg_v16_add_column_if_missing('strengths', 'TEXT');
CALL gpg_v16_add_column_if_missing('weaknesses', 'TEXT');
CALL gpg_v16_add_column_if_missing('follow_up_issue', 'TEXT');
CALL gpg_v16_add_column_if_missing('follow_up_status', 'VARCHAR(60)');

DROP PROCEDURE gpg_v16_add_column_if_missing;

-- Đóng khung hiệu lực của các phiên bản KPI đã bị thay thế.
--
-- V21 seed GPG-CD-KPI-V1 (31 mã), V22 seed GPG-CD-KPI-V2 (23 mã) và đánh V1 là SUPERSEDED,
-- V23 seed GPG-CD-KPI-V3 (24 mã) và đánh V2 là SUPERSEDED. Cả ba đều mang
-- effective_from = '2026-01-01' và effective_to = NULL, nghĩa là ba khung hiệu lực trùng nhau
-- hoàn toàn. GpgKpiEngine.activeVersion() ném lỗi khi có nhiều hơn một phiên bản chọn được
-- (status ACTIVE hoặc RETIRED) bao phủ cùng một kỳ, nên hiện tại toàn bộ module KPI chỉ chạy
-- được nhờ đúng một điều: cột status của V1 và V2 đang là SUPERSEDED. Ai đó bật lại status của
-- chúng — ví dụ để xem điểm kỳ cũ — là GET /api/kpi trả 500 cho mọi đơn vị.
--
-- Đóng khung tại đúng ngày bắt đầu để bất biến này không còn phụ thuộc vào một cột trạng thái:
-- kỳ nhỏ nhất mà engine hỗ trợ là MONTH (period_end là ngày cuối tháng), nên effective_to
-- ngày 2026-01-01 khiến hai phiên bản cũ không thể được chọn cho bất kỳ kỳ nào, kể cả khi
-- status bị đổi. Điều kiện effective_to >= effective_from mà validateVersion() yêu cầu vẫn đúng.
--
-- Lịch sử điểm không bị ảnh hưởng: KpiHistoryController đọc từ kpi_runs, không chọn lại phiên bản.
UPDATE kpi_versions
SET effective_to = effective_from,
    updated_at = CURRENT_TIMESTAMP
WHERE version_id IN ('GPG-CD-KPI-V1', 'GPG-CD-KPI-V2')
  AND effective_to IS NULL;

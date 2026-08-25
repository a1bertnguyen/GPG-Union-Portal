# Đối chiếu requirement — GPG Union Portal

Nguồn đối chiếu: `SỐ HOÁ_CONG_DOAN_GPG_2026.pptx` và mã nguồn hiện tại, cập nhật ngày 25/08/2026.

## Kết luận

Hệ thống đã đủ **khung MVP vận hành nội bộ**: đăng nhập JWT, phạm vi dữ liệu theo đơn vị, dữ liệu CĐCS/đoàn viên/chăm lo/vụ việc/hoạt động/tài chính, khảo sát, dashboard, báo cáo và nhập Excel. Tuy nhiên, **chưa thể coi là hoàn thành toàn bộ requirement trong PowerPoint** vì còn thiếu mô hình vai trò nghiệp vụ, kho tài liệu/tệp đính kèm và một số workflow chi tiết.

## Ma trận phạm vi

| Nhóm | Trạng thái | Đã có | Còn thiếu / cần làm rõ |
|---|---|---|---|
| Đăng nhập nội bộ | Hoàn thành MVP | JWT, BCrypt, hết hạn phiên, khóa tài khoản | Chính sách quên/đổi mật khẩu và nhật ký đăng nhập nếu triển khai production |
| Phân quyền | Một phần | `ADMIN`, `USER`; USER bị giới hạn theo CĐCS | Các vai trò trong đề xuất: CĐCS, Công đoàn GPG, Ban CSNLĐ, Ban lãnh đạo; quyền xem/duyệt theo từng phân hệ |
| CĐCS / BCH | Gần hoàn thành | Hồ sơ đơn vị, nhiệm kỳ, quyết định, đầu mối, trạng thái | Tệp quyết định và lịch sử thay đổi BCH |
| Đoàn viên / NLĐ | Một phần | Danh sách, trạng thái công đoàn/nhân sự, CRUD, CSV/Excel | Tab biến động có lịch sử; kho tài liệu hồ sơ; tệp đính kèm |
| Chăm lo / phúc lợi | Một phần | Loại chăm lo, người thụ hưởng, số tiền, trạng thái, trạng thái chứng từ | Danh mục chính sách/định mức; deadline riêng; nhắc tự động; luồng duyệt và tệp chứng từ |
| Kiến nghị / vụ việc | Một phần | Phân loại, mức độ, PIC, deadline, trạng thái, kết quả, lý do quá hạn | Nguồn tiếp nhận; nhật ký từng bước xử lý; trao đổi/phản hồi; tệp bằng chứng; dashboard phân tích sâu |
| Hoạt động công đoàn | Một phần | Kế hoạch, ngân sách, chi phí, tham dự, điểm hữu ích, follow-up, cờ báo cáo | Số người mời/tỷ lệ tham dự; check-in; nội dung thực tế; đầu ra/kết quả; bài học; hình ảnh và tài liệu |
| Tài chính nội bộ | Hoàn thành MVP | Nhập thu/chi, chứng từ, tổng thu/chi/số dư; không kết nối ngân hàng | Tạm ứng/quyết toán nếu đây là nghiệp vụ bắt buộc; tải tệp chứng từ |
| Báo cáo M01 | Một phần | Chỉ số tháng, người lập, trạng thái, kế hoạch, đề xuất hỗ trợ, Excel/CSV | Nội dung đã làm/chưa hoàn tất, PIC, ETA và hỗ trợ theo đúng cấu trúc biểu mẫu đề xuất |
| Báo cáo M02 hoạt động | Chưa đủ | Dữ liệu hoạt động có thể tổng hợp cơ bản | Mẫu M02 riêng và quy trình lập/nộp/duyệt |
| Employee Voice | Hoàn thành MVP | Khảo sát, phản hồi ẩn danh, điểm kết nối, top nhu cầu, cảnh báo | Phân tích xu hướng nhiều kỳ và quyền xem chi tiết theo vai trò |
| Dashboard | Gần hoàn thành về UI | Sáu dashboard độc lập: điều hành, chăm lo, vụ việc, hoạt động, tài chính và tiếng nói NLĐ | Một số KPI hiện được tính tại frontend; cần API tổng hợp riêng, công thức KPI được duyệt và dashboard theo đúng vai trò nghiệp vụ |
| Import Excel | Hoàn thành MVP | Mẫu và import `.xlsx` cho các bảng nhập liệu chính, có báo lỗi theo dòng | Quy tắc đối soát/ngăn trùng nâng cao và màn hình xem trước trước khi ghi dữ liệu |
| Kho tài liệu | Chưa có | Có trường trạng thái chứng từ | Lưu tệp, metadata, phân quyền tải/xem, phiên bản và liên kết tài liệu với hồ sơ |
| Thông báo / nhắc hạn | Chưa đủ | Cảnh báo được tính và hiển thị trên dashboard | Bộ lập lịch gửi nhắc, người nhận, trạng thái đã đọc và cấu hình ngưỡng |

## Thứ tự nên triển khai tiếp

1. Chốt ma trận vai trò và quyền: CĐCS, Công đoàn GPG, Ban CSNLĐ, Ban lãnh đạo.
2. Làm kho tài liệu/tệp đính kèm dùng chung cho đoàn viên, chăm lo, vụ việc, hoạt động và tài chính.
3. Chuẩn hóa biểu mẫu M01 và thêm M02 đúng trường thông tin trong đề xuất.
4. Bổ sung lịch sử workflow, deadline/nhắc việc và nhật ký xử lý.
5. Hoàn thiện API chỉ số cho sáu dashboard và kiểm thử UAT theo từng vai trò.

## Khoảng trống nghiệp vụ phát hiện thêm

Ngoài các trường dữ liệu trên màn hình, để vận hành thực tế cần chốt thêm các quy tắc sau:

1. **SLA và escalation:** thời hạn theo loại/mức độ vụ việc, người nhận escalation và cơ chế gia hạn có phê duyệt.
2. **Luồng duyệt:** người lập, người kiểm tra, người phê duyệt và điều kiện trả lại hồ sơ cho chăm lo, tài chính, hoạt động và báo cáo.
3. **Lịch sử thay đổi:** ai thay đổi trường nào, thời điểm, giá trị trước/sau và lý do chỉnh sửa.
4. **Quản trị dữ liệu trùng:** quy tắc nhận diện trùng đoàn viên, trùng đối tượng chăm lo, trùng vụ việc và cách hợp nhất bản ghi.
5. **Danh mục dùng chung:** nhóm vấn đề, loại chính sách, định mức, nhóm thu/chi, loại hoạt động và phiên bản hiệu lực.
6. **Chốt kỳ dữ liệu:** khóa báo cáo/tài chính sau khi duyệt; mở khóa phải có quyền và lý do.
7. **Lịch công việc:** giao việc, người phối hợp, nhắc hạn, xác nhận hoàn tất và bảng công việc cá nhân.
8. **Bằng chứng nghiệp vụ:** tệp đính kèm, loại tài liệu, phiên bản, người tải lên và thời hạn lưu trữ.
9. **Đồng thuận và bảo mật phản hồi:** quy định ẩn danh, ngưỡng tối thiểu trước khi hiển thị thống kê và hạn chế truy ngược người trả lời.
10. **Kịch bản vận hành lỗi:** file Excel lỗi một phần, hoàn tác lượt nhập, dữ liệu thiếu đơn vị và quy trình sửa sai.

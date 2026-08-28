type MemberOption = { value: string; label: string }

const options = (values: readonly string[]): MemberOption[] => values.map(value => ({ value, label: value }))

// Danh mục lấy từ cột “Công ty” của Book1.xlsx (cập nhật 30/07/2026).
// Đây là dữ liệu quy định cho form đoàn viên, không cho nhập tự do.
export const memberCompanyOptions = options([
  'CÔNG TY CỔ PHẦN CẢNG VIỆT NAM',
  'CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ',
  'Công ty Cổ phần Doanh nhân khởi nghiệp Phú Yên',
  'CÔNG TY CỔ PHẦN ĐỐI TÁC CHÂN THẬT',
  'CÔNG TY CỔ PHẦN KHAI THÁC XÂY DỰNG HƯNG THÁI',
  'CÔNG TY CỔ PHẦN TẬP ĐOÀN VỮNG AN',
  'CÔNG TY CỔ PHẦN THT E-LOGISTICS',
  'Công ty Cổ Phần Ứng Dụng Công Nghệ Logistics',
  'CÔNG TY TNHH CỘNG ĐỒNG CÔNG TÁC XÃ HỘI VIỆT NAM',
  'CÔNG TY TNHH GIẢI PHÁP CONTAINER VN',
  'CÔNG TY TNHH KHAI THÁC CẢNG CÁI MÉP THỊ VẢI',
  'CÔNG TY TNHH MTV ĐẦU TƯ LOGISTICS MIỀN TRUNG',
  'CÔNG TY TNHH MTV LOGISTICS ĐỐI TÁC CHÂN THẬT',
  'CÔNG TY TNHH QUẢN LÝ NỀN TẢNG KẾT NỐI',
  'CÔNG TY TNHH TẬP ĐOÀN ĐỐI TÁC CHÂN THẬT',
  'CÔNG TY TNHH TRUYỀN THÔNG QUỐC TẾ CHÂN THẬT',
  'CÔNG TY TNHH TƯ VẤN ĐẦU TƯ ĐÔNG SÀI GÒN',
  'GENUINE PARTNER LOGISTICS (CAMBODIA) CO., LTD',
] as const)

// Cột F trong Book1.xlsx là giá trị lưu; cột E chỉ là tên gọi dễ đọc.
// Một số dòng nguồn dùng nguyên tên làm mã, vì vậy các giá trị đó vẫn được giữ nguyên.
export const memberWorkplaceOptions: MemberOption[] = [
  { value: 'AND', label: 'AND · An Đồn' },
  { value: 'BSD', label: 'BSD · BNP SÓNG THẦN DEPOT' },
  { value: 'CMTV', label: 'CMTV · CÁI MÉP' },
  { value: 'CLD', label: 'CLD · CHÂN THẬT LONG THẠNH MỸ DEPOT' },
  { value: 'CHD', label: 'CHD · CHÂN THẬT HẢI PHÒNG DEPOT' },
  { value: 'DAD', label: 'DAD · CHÂN THẬT TIÊN SA DEPOT' },
  { value: 'CPHA', label: 'CPHA · CHÂU PHA' },
  { value: 'ETD', label: 'ETD · E - TÂN BÌNH DEPOT' },
  { value: 'LLC', label: 'LLC · E-DEPOT LINH XUÂN' },
  { value: 'GKD', label: 'GKD' },
  { value: 'SWD', label: 'SWD · SNP WAREHOUSE DEPOT' },
  { value: 'TBD', label: 'TBD · TÂY NAM BÌNH DƯƠNG DEPOT' },
  { value: 'VP-TCT', label: 'VP-TCT · VĂN PHÒNG TỔNG CÔNG TY' },
  { value: 'THT2', label: 'THT2 · Nhà Anh Sở' },
  { value: 'NT', label: 'NT · Nha Trang' },
  { value: 'PY-LTK', label: 'PY-LTK · Nhạn Trà Từ Quán' },
  { value: 'PY-AD', label: 'PY-AD · NÚI AN ĐỊNH' },
  { value: 'PY-CT', label: 'PY-CT · NÚI CHÍ THẠNH' },
  { value: 'TN', label: 'TN · TÂY NINH' },
  { value: 'BIỆT THỰ', label: 'BIỆT THỰ' },
  { value: 'BNP SÓNG THẦN DEPOT', label: 'BNP SÓNG THẦN DEPOT' },
  { value: 'CHÂN THẬT HẢI PHÒNG DEPOT', label: 'CHÂN THẬT HẢI PHÒNG DEPOT' },
  { value: 'CHÂN THẬT LONG THẠNH MỸ DEPOT', label: 'CHÂN THẬT LONG THẠNH MỸ DEPOT' },
  { value: 'CỘNG ĐỒNG CÔNG TÁC XÃ HỘI VN', label: 'CỘNG ĐỒNG CÔNG TÁC XÃ HỘI VN' },
  { value: 'E-DEPOT LINH XUÂN', label: 'E-DEPOT LINH XUÂN' },
  { value: 'GP - CAMBODIA', label: 'GP - CAMBODIA' },
  { value: 'SNP WAREHOUSE DEPOT', label: 'SNP WAREHOUSE DEPOT' },
  { value: 'TRAPANG KRASANG DEPOT', label: 'TRAPANG KRASANG DEPOT' },
  { value: 'VĂN PHÒNG TỔNG CÔNG TY', label: 'VĂN PHÒNG TỔNG CÔNG TY' },
]

export const memberEducationOptions = options([
  'Không có',
  '12/12',
  'Trung cấp',
  'Cao đẳng',
  'Đại học',
  'Trên đại học',
] as const)

export const memberPoliticalTheoryOptions = options([
  'Chưa đào tạo',
  'Sơ cấp',
  'Trung cấp',
  'Cao cấp',
  'Cử nhân',
] as const)

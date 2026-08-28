package vn.gpg.unionportal.validation;

import java.util.Set;

/** Controlled member-profile values transcribed from Book1.xlsx. */
public final class MemberProfileCatalog {
    private static final Set<String> COMPANIES = Set.of(
            "CÔNG TY CỔ PHẦN CẢNG VIỆT NAM",
            "CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ",
            "Công ty Cổ phần Doanh nhân khởi nghiệp Phú Yên",
            "CÔNG TY CỔ PHẦN ĐỐI TÁC CHÂN THẬT",
            "CÔNG TY CỔ PHẦN KHAI THÁC XÂY DỰNG HƯNG THÁI",
            "CÔNG TY CỔ PHẦN TẬP ĐOÀN VỮNG AN",
            "CÔNG TY CỔ PHẦN THT E-LOGISTICS",
            "Công ty Cổ Phần Ứng Dụng Công Nghệ Logistics",
            "CÔNG TY TNHH CỘNG ĐỒNG CÔNG TÁC XÃ HỘI VIỆT NAM",
            "CÔNG TY TNHH GIẢI PHÁP CONTAINER VN",
            "CÔNG TY TNHH KHAI THÁC CẢNG CÁI MÉP THỊ VẢI",
            "CÔNG TY TNHH MTV ĐẦU TƯ LOGISTICS MIỀN TRUNG",
            "CÔNG TY TNHH MTV LOGISTICS ĐỐI TÁC CHÂN THẬT",
            "CÔNG TY TNHH QUẢN LÝ NỀN TẢNG KẾT NỐI",
            "CÔNG TY TNHH TẬP ĐOÀN ĐỐI TÁC CHÂN THẬT",
            "CÔNG TY TNHH TRUYỀN THÔNG QUỐC TẾ CHÂN THẬT",
            "CÔNG TY TNHH TƯ VẤN ĐẦU TƯ ĐÔNG SÀI GÒN",
            "GENUINE PARTNER LOGISTICS (CAMBODIA) CO., LTD");

    private static final Set<String> WORKPLACES = Set.of(
            "AND", "BSD", "CMTV", "CLD", "CHD", "DAD", "CPHA", "ETD", "LLC", "GKD",
            "SWD", "TBD", "VP-TCT", "THT2", "NT", "PY-LTK", "PY-AD", "PY-CT", "TN",
            "BIỆT THỰ", "BNP SÓNG THẦN DEPOT", "CHÂN THẬT HẢI PHÒNG DEPOT",
            "CHÂN THẬT LONG THẠNH MỸ DEPOT", "CỘNG ĐỒNG CÔNG TÁC XÃ HỘI VN",
            "E-DEPOT LINH XUÂN", "GP - CAMBODIA", "SNP WAREHOUSE DEPOT",
            "TRAPANG KRASANG DEPOT", "VĂN PHÒNG TỔNG CÔNG TY");

    private MemberProfileCatalog() {
    }

    public static void validate(String company, String workplace) {
        requireAllowed("Công ty", company, COMPANIES);
        requireAllowed("Nơi làm việc", workplace, WORKPLACES);
    }

    public static Set<String> companies() {
        return COMPANIES;
    }

    public static Set<String> workplaces() {
        return WORKPLACES;
    }

    private static void requireAllowed(String field, String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return;
        if (!allowed.contains(value.trim())) {
            throw new IllegalArgumentException(field + " phải được chọn từ danh mục quy định.");
        }
    }
}

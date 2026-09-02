package vn.gpg.unionportal.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vietnamese labels for every domain enum constant.
 *
 * <p>These used to live only in {@code frontend/src/api.ts}. Server-side search needs them too:
 * typing "Chờ duyệt" into the search box has always matched {@code PENDING_APPROVAL} rows, and moving
 * filtering to the database would silently drop that behaviour. This class is the single source of
 * truth — the frontend hydrates its display map from {@code GET /api/meta/enum-labels} so the two
 * cannot drift apart.
 */
public final class EnumLabels {
    private static final Locale VI = Locale.forLanguageTag("vi");

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ACTIVE", "Đang hoạt động"),
            Map.entry("INACTIVE", "Ngừng hoạt động"),
            Map.entry("MEMBER", "Đoàn viên"),
            Map.entry("NOT_JOINED", "Chưa gia nhập"),
            Map.entry("LEFT", "Đã rời"),
            Map.entry("BIRTHDAY", "Sinh nhật"),
            Map.entry("FUNERAL", "Hiếu"),
            Map.entry("WEDDING", "Hỷ"),
            Map.entry("VISIT", "Thăm hỏi"),
            Map.entry("CHILDBIRTH", "Sinh con"),
            Map.entry("HARDSHIP", "Khó khăn"),
            Map.entry("UNION", "Công đoàn hỗ trợ"),
            Map.entry("COMPANY", "Công ty hỗ trợ"),
            Map.entry("NEW", "Mới"),
            Map.entry("PENDING_APPROVAL", "Chờ duyệt"),
            Map.entry("IN_PROGRESS", "Đang xử lý"),
            Map.entry("COMPLETED", "Hoàn tất"),
            Map.entry("CANCELLED", "Đã hủy"),
            Map.entry("COMPLETE", "Đủ"),
            Map.entry("INCOMPLETE", "Chưa đủ"),
            Map.entry("NOT_REQUIRED", "Không yêu cầu"),
            Map.entry("LOW", "Thấp"),
            Map.entry("MEDIUM", "Trung bình"),
            Map.entry("HIGH", "Cao"),
            Map.entry("CRITICAL", "Khẩn cấp"),
            Map.entry("VERIFYING", "Đang xác minh"),
            Map.entry("WAITING_RESPONSE", "Chờ phản hồi"),
            Map.entry("CLOSED", "Đã đóng"),
            Map.entry("CLASSIFYING", "Đang phân loại"),
            Map.entry("ASSIGNED", "Đã giao PIC"),
            Map.entry("PLANNED", "Kế hoạch"),
            Map.entry("APPROVED", "Đã duyệt"),
            Map.entry("INCOME", "Thu"),
            Map.entry("EXPENSE", "Chi"),
            Map.entry("ADVANCE", "Tạm ứng"),
            Map.entry("DRAFT", "Bản nháp"),
            Map.entry("SUBMITTED", "Đã nộp"),
            Map.entry("HR_IMPORT", "Nhập dữ liệu HR"),
            Map.entry("FINANCE_IMPORT", "Nhập dữ liệu tài chính"),
            Map.entry("UNITS_IMPORT", "Nhập CĐCS từ Excel"),
            Map.entry("MEMBERS_IMPORT", "Nhập đoàn viên từ Excel"),
            Map.entry("WELFARE_IMPORT", "Nhập chăm lo từ Excel"),
            Map.entry("WELFARE_POLICIES_IMPORT", "Nhập chính sách chăm lo từ Excel"),
            Map.entry("CASES_IMPORT", "Nhập kiến nghị từ Excel"),
            Map.entry("ACTIVITIES_IMPORT", "Nhập chương trình từ Excel"),
            Map.entry("FINANCE_EXCEL_IMPORT", "Nhập tài chính từ Excel"),
            Map.entry("SURVEYS_IMPORT", "Nhập khảo sát từ Excel"),
            Map.entry("SURVEY_RESPONSES_IMPORT", "Nhập phản hồi khảo sát từ Excel"),
            Map.entry("REPORTS_IMPORT", "Nhập báo cáo từ Excel"),
            Map.entry("USERS_IMPORT", "Nhập tài khoản từ Excel"),
            Map.entry("PARTIAL", "Hoàn tất một phần"),
            Map.entry("FAILED", "Thất bại"),
            Map.entry("JOIN_APPLICATION", "Đơn gia nhập"),
            Map.entry("DECISION", "Quyết định"),
            Map.entry("BCH_DOCUMENT", "Tài liệu BCH"),
            Map.entry("SUPPORTING_DOCUMENT", "Hồ sơ / chứng từ"),
            Map.entry("RECEIPT", "Biên nhận / quyết toán"),
            Map.entry("IMAGE", "Hình ảnh"),
            Map.entry("PHOTO", "Ảnh"),
            Map.entry("DOCUMENT", "Tài liệu"));

    private EnumLabels() {
    }

    public static Map<String, String> all() {
        return LABELS;
    }

    /** Vietnamese label for an enum constant, falling back to the constant name. */
    public static String label(String constant) {
        return LABELS.getOrDefault(constant, constant);
    }

    public static String label(Enum<?> value) {
        return value == null ? "" : label(value.name());
    }

    /**
     * Enum constants of {@code type} whose constant name or Vietnamese label contains {@code text}.
     *
     * <p>Matching is a plain lowercase substring test — diacritics are not stripped — so it behaves
     * exactly like the {@code toLocaleLowerCase('vi').includes(...)} check the frontend used to do.
     * Returns an empty list when nothing matches, which callers must read as "no rows", not "no filter".
     */
    public static <E extends Enum<E>> List<E> matching(Class<E> type, String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(VI);
        if (needle.isEmpty()) return List.of();
        List<E> matches = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().toLowerCase(VI).contains(needle)
                    || label(constant.name()).toLowerCase(VI).contains(needle)) {
                matches.add(constant);
            }
        }
        return matches;
    }
}

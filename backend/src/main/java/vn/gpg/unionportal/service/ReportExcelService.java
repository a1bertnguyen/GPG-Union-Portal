package vn.gpg.unionportal.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.model.ActivityMedia;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.ActivityMediaRepository;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.MonthlyReportRepository;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.repository.WelfareRecordRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Produces read-only workbooks from the detail records already held by the portal.
 * The service deliberately scopes every query through {@link CurrentUserService},
 * so an export cannot bypass the unit boundary used by the rest of the application.
 */
@Service
public class ReportExcelService {
    private final CurrentUserService currentUser;
    private final UnionUnitRepository unitRepository;
    private final MemberRepository memberRepository;
    private final MonthlyReportRepository reportRepository;
    private final UnionActivityRepository activityRepository;
    private final ActivityMediaRepository activityMediaRepository;
    private final WelfareRecordRepository welfareRepository;
    private final LaborCaseRepository caseRepository;
    private final FinanceEntryRepository financeRepository;

    public ReportExcelService(CurrentUserService currentUser,
                              UnionUnitRepository unitRepository,
                              MemberRepository memberRepository,
                              MonthlyReportRepository reportRepository,
                              UnionActivityRepository activityRepository,
                              ActivityMediaRepository activityMediaRepository,
                              WelfareRecordRepository welfareRepository,
                              LaborCaseRepository caseRepository,
                              FinanceEntryRepository financeRepository) {
        this.currentUser = currentUser;
        this.unitRepository = unitRepository;
        this.memberRepository = memberRepository;
        this.reportRepository = reportRepository;
        this.activityRepository = activityRepository;
        this.activityMediaRepository = activityMediaRepository;
        this.welfareRepository = welfareRepository;
        this.caseRepository = caseRepository;
        this.financeRepository = financeRepository;
    }

    /** Workbook for the monthly, quarterly, or yearly reporting process; currently the UI supplies a month. */
    public byte[] exportPeriodicReport(String monthValue, Long requestedUnitId) {
        return exportSummary(monthValue, requestedUnitId, "BÁO CÁO CÔNG ĐOÀN ĐỊNH KỲ");
    }

    /** Company-level view of the same single-source data, with one summary and four detail sheets. */
    public byte[] exportCompanySummary(String monthValue, Long requestedUnitId) {
        return exportSummary(monthValue, requestedUnitId, "BÁO CÁO TỔNG HỢP CÔNG ĐOÀN CÔNG TY");
    }

    public byte[] exportActivityReport(Long activityId) {
        UnionActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình cần xuất báo cáo"));
        currentUser.requireUnitAccess(activity.getUnionUnit().getId());

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Báo cáo sau CT");
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 3);
            createTitle(sheet, "BÁO CÁO SAU CHƯƠNG TRÌNH", 3, styles);
            setText(sheet.createRow(1), 0, "Mã chương trình: " + activity.getActivityCode()
                    + " · CĐCS: " + activity.getUnionUnit().getCode(), styles.note);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));
            createHeader(sheet.createRow(3), new String[]{"Nhóm dữ liệu", "Trường dữ liệu", "Giá trị", "Ghi chú / KPI"}, styles);

            int row = 4;
            row = detailRow(sheet, row, "Định danh", "Tên chương trình", activity.getName(), "", styles);
            row = detailRow(sheet, row, "Định danh", "Công ty / CĐCS", activity.getUnionUnit().getCompanyName()
                    + " / " + activity.getUnionUnit().getName(), "", styles);
            row = detailRow(sheet, row, "Định danh", "Ngày, giờ và địa điểm", join(date(activity.getEventDate()), time(activity.getEventTime()), activity.getLocation()), "", styles);
            row = detailRow(sheet, row, "Định danh", "Người phụ trách", activity.getProgramPic(), "", styles);
            row = detailRow(sheet, row, "Kế hoạch", "Mục tiêu", activity.getObjective(), "", styles);
            row = detailRow(sheet, row, "Kế hoạch", "Nhóm người lao động", activity.getEmployeeGroup(), "", styles);
            row = detailRow(sheet, row, "Kế hoạch", "Ngân sách được duyệt", money(activity.getPlannedBudget()), "", styles);
            row = detailRow(sheet, row, "Thực tế", "Số người tham gia", number(activity.getParticipantCount()), "Mời: " + number(activity.getInvitedCount()), styles);
            row = detailRow(sheet, row, "Thực tế", "Nội dung đã triển khai", activity.getActualContent(), "", styles);
            row = detailRow(sheet, row, "Tài chính", "Chi phí thực tế", money(activity.getActualCost()), "", styles);
            row = detailRow(sheet, row, "KPI tự động", "Tỷ lệ tham gia", percent(activity.getParticipantCount(), activity.getInvitedCount()), "Tham gia / số người mời", styles);
            row = detailRow(sheet, row, "KPI tự động", "Chênh lệch ngân sách", money(subtract(activity.getPlannedBudget(), activity.getActualCost())), "Ngân sách duyệt − chi thực tế", styles);
            row = detailRow(sheet, row, "KPI tự động", "Tỷ lệ sử dụng ngân sách", percent(activity.getActualCost(), activity.getPlannedBudget()), "Chi thực tế / ngân sách duyệt", styles);
            row = detailRow(sheet, row, "KPI tự động", "Chi phí bình quân/người", money(divide(activity.getActualCost(), activity.getParticipantCount())), "Chi thực tế / số người tham gia", styles);
            row = detailRow(sheet, row, "Đánh giá", "Điểm hữu ích / hài lòng", number(activity.getUsefulnessScore()), "Thang điểm 5", styles);
            row = detailRow(sheet, row, "Đánh giá", "Khác biệt so với kế hoạch", activity.getPlanDifference(), "", styles);
            row = detailRow(sheet, row, "Đánh giá", "Phản hồi và vấn đề", join(activity.getQuickFeedback(), activity.getIssues()), "", styles);
            row = detailRow(sheet, row, "Hiệu quả", "Kết quả / đề xuất", activity.getOutputProposal(), "", styles);
            row = detailRow(sheet, row, "Hiệu quả", "Điều làm tốt / cần cải thiện", join(activity.getStrengths(), activity.getWeaknesses()), "", styles);
            row = detailRow(sheet, row, "Hiệu quả", "Bài học kinh nghiệm", activity.getLessonsLearned(), "", styles);
            row = detailRow(sheet, row, "Theo dõi", "Vấn đề follow-up", activity.getFollowUpIssue(), "PIC: " + text(activity.getFollowUpOwner()) + " · Hạn: " + date(activity.getFollowUpDeadline()), styles);
            detailRow(sheet, row, "Theo dõi", "Trạng thái / chứng từ", text(activity.getFollowUpStatus())
                    + " / " + text(activity.getDocumentStatus()), "Hoàn tất báo cáo: " + yesNo(activity.getReportCompleted()), styles);
            setWidths(sheet, 22, 30, 70, 36);

            Sheet evidence = workbook.createSheet("Hồ sơ đính kèm");
            evidence.setDisplayGridlines(false);
            createTitle(evidence, "HỒ SƠ, HÌNH ẢNH VÀ CHỨNG TỪ", 4, styles);
            createHeader(evidence.createRow(2), new String[]{"Loại", "Tên hiển thị", "Tên tệp", "Người tải", "Dung lượng (byte)"}, styles);
            List<ActivityMedia> files = activityMediaRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
            for (int index = 0; index < files.size(); index++) {
                ActivityMedia file = files.get(index);
                Row data = evidence.createRow(index + 3);
                setText(data, 0, text(file.getMediaType()), styles.body);
                setText(data, 1, file.getTitle(), styles.body);
                setText(data, 2, file.getFileName(), styles.body);
                setText(data, 3, file.getUploadedBy(), styles.body);
                setText(data, 4, number(file.getFileSize()), styles.body);
            }
            evidence.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, files.size() + 2), 0, 4));
            setWidths(evidence, 18, 34, 44, 26, 18);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo báo cáo sau chương trình dạng Excel", exception);
        }
    }

    public byte[] exportCaseBook() {
        Long scope = currentUser.scopedUnitId(null);
        List<LaborCase> cases = caseRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope))
                .sorted(Comparator.comparing(LaborCase::getReceivedDate).reversed().thenComparing(LaborCase::getCaseCode))
                .toList();

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Sổ kiến nghị");
            sheet.setDisplayGridlines(false);
            createTitle(sheet, "SỔ KIẾN NGHỊ, PHẢN ÁNH CỦA NGƯỜI LAO ĐỘNG", 20, styles);
            setText(sheet.createRow(1), 0, "Dữ liệu được xuất theo phạm vi CĐCS của tài khoản hiện tại.", styles.note);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 20));
            String[] headers = {"Mã kiến nghị", "Ngày tiếp nhận", "CĐCS", "Công ty", "Người kiến nghị", "Mã nhân viên",
                    "Chức danh", "Nơi làm việc", "Điện thoại", "Kênh tiếp nhận", "Nhóm kiến nghị", "Mức độ ưu tiên",
                    "Người xử lý", "Hạn xử lý", "Trạng thái", "Nội dung", "Kết quả xử lý", "Ngày phản hồi",
                    "Quá hạn / ghi chú", "Người duyệt", "Ngày duyệt"};
            createHeader(sheet.createRow(3), headers, styles);
            for (int index = 0; index < cases.size(); index++) {
                LaborCase item = cases.get(index);
                Row row = sheet.createRow(index + 4);
                String overdue = item.getDeadline() != null && item.getDeadline().isBefore(LocalDate.now())
                        && !"CLOSED".equals(text(item.getStatus())) ? "Quá hạn" : "Đúng hạn/chưa đến hạn";
                String[] values = {item.getCaseCode(), date(item.getReceivedDate()), item.getUnionUnit().getCode(), item.getUnionUnit().getCompanyName(),
                        item.getRequesterName(), item.getEmployeeCode(), item.getJobTitle(), item.getWorkplace(), item.getPhone(), item.getSource(),
                        item.getIssueGroup(), text(item.getSeverity()), item.getOwnerName(), date(item.getDeadline()), text(item.getStatus()),
                        item.getDescription(), item.getResultText(), date(item.getResponseDate()), overdue + appendNote(item.getOverdueReason()),
                        item.getApprovedBy(), date(item.getApprovedAt())};
                writeRow(row, values, styles.body);
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, Math.max(4, cases.size() + 3), 0, headers.length - 1));
            setWidths(sheet, 18, 16, 14, 24, 25, 16, 22, 22, 16, 18, 22, 16, 22, 16, 18, 56, 56, 16, 38, 22, 20);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo sổ kiến nghị dạng Excel", exception);
        }
    }

    private byte[] exportSummary(String monthValue, Long requestedUnitId, String title) {
        YearMonth month = parseMonth(monthValue);
        Long scope = currentUser.scopedUnitId(requestedUnitId);
        SummaryData data = loadSummaryData(month, scope);

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            createSummarySheet(workbook, data, title, styles);
            createMonthlyReportSheet(workbook, data, styles);
            createActivitySheet(workbook, data, styles);
            createWelfareSheet(workbook, data, styles);
            createCaseSheet(workbook, data, styles);
            createFinanceSheet(workbook, data, styles);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo báo cáo tổng hợp dạng Excel", exception);
        }
    }

    private SummaryData loadSummaryData(YearMonth month, Long scope) {
        List<UnionUnit> units = unitRepository.findAll().stream()
                .filter(unit -> inScope(unit, scope))
                .sorted(Comparator.comparing(UnionUnit::getCode))
                .toList();
        List<Member> members = memberRepository.findAll().stream().filter(item -> inScope(item.getUnionUnit(), scope)).toList();
        List<MonthlyReport> reports = reportRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope)).filter(item -> month.equals(YearMonth.from(item.getReportMonth()))).toList();
        List<UnionActivity> activities = activityRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope)).filter(item -> isInMonth(item.getEventDate(), month)).toList();
        List<WelfareRecord> welfare = welfareRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope)).filter(item -> isInMonth(item.getEventDate(), month)).toList();
        List<LaborCase> cases = caseRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope)).filter(item -> isInMonth(item.getReceivedDate(), month)).toList();
        List<FinanceEntry> finance = financeRepository.findAll().stream()
                .filter(item -> inScope(item.getUnionUnit(), scope)).filter(item -> isInMonth(item.getTransactionDate(), month)).toList();
        return new SummaryData(month, units, members, reports, activities, welfare, cases, finance);
    }

    private void createSummarySheet(XSSFWorkbook workbook, SummaryData data, String title, Styles styles) {
        Sheet sheet = workbook.createSheet("Tổng hợp");
        sheet.setDisplayGridlines(false);
        createTitle(sheet, title + " · " + data.month(), 14, styles);
        setText(sheet.createRow(1), 0, "Nguồn dữ liệu: đoàn viên, chương trình, chăm lo, kiến nghị, tài chính và báo cáo định kỳ.", styles.note);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 14));
        String[] headers = {"Mã CĐCS", "Công ty", "Kỳ báo cáo", "Người lập", "Trạng thái", "Tổng NLĐ", "Đoàn viên",
                "Chương trình", "Chăm lo", "Kiến nghị", "Thu", "Chi", "Chênh lệch", "Kế hoạch kỳ tới", "Đề xuất / kiến nghị"};
        createHeader(sheet.createRow(3), headers, styles);
        Map<Long, MonthlyReport> reportsByUnit = data.reports().stream().collect(Collectors.toMap(
                item -> item.getUnionUnit().getId(), Function.identity(), (left, right) -> left));
        for (int index = 0; index < data.units().size(); index++) {
            UnionUnit unit = data.units().get(index);
            MonthlyReport report = reportsByUnit.get(unit.getId());
            long employees = data.members().stream().filter(item -> item.getUnionUnit().getId().equals(unit.getId()))
                    .filter(item -> "ACTIVE".equals(text(item.getEmploymentStatus()))).count();
            long unionMembers = data.members().stream().filter(item -> item.getUnionUnit().getId().equals(unit.getId()))
                    .filter(item -> "MEMBER".equals(text(item.getMembershipStatus()))).count();
            long activities = data.activities().stream().filter(item -> item.getUnionUnit().getId().equals(unit.getId())).count();
            long welfare = data.welfare().stream().filter(item -> item.getUnionUnit().getId().equals(unit.getId())).count();
            long cases = data.cases().stream().filter(item -> item.getUnionUnit().getId().equals(unit.getId())).count();
            BigDecimal income = sumFinance(data.finance(), unit.getId(), "INCOME");
            BigDecimal expense = sumFinance(data.finance(), unit.getId(), "EXPENSE");
            Row row = sheet.createRow(index + 4);
            String[] values = {unit.getCode(), unit.getCompanyName(), data.month().toString(), report == null ? "" : report.getPreparedBy(),
                    report == null ? "CHƯA NỘP" : text(report.getStatus()), String.valueOf(employees), String.valueOf(unionMembers),
                    String.valueOf(activities), String.valueOf(welfare), String.valueOf(cases), money(income), money(expense),
                    money(subtract(income, expense)), report == null ? "" : report.getPlanNextMonth(), report == null ? "" : report.getSupportRequest()};
            writeRow(row, values, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, Math.max(4, data.units().size() + 3), 0, headers.length - 1));
        setWidths(sheet, 14, 25, 15, 22, 18, 13, 13, 15, 13, 14, 18, 18, 18, 46, 46);
    }

    private void createMonthlyReportSheet(XSSFWorkbook workbook, SummaryData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Báo cáo định kỳ");
        createTitle(sheet, "HỒ SƠ BÁO CÁO ĐỊNH KỲ", 7, styles);
        String[] headers = {"Mã CĐCS", "Công ty", "Kỳ", "Người lập", "Ngày nộp", "Kế hoạch kỳ tới", "Đề xuất / kiến nghị", "Trạng thái"};
        createHeader(sheet.createRow(2), headers, styles);
        List<MonthlyReport> reports = data.reports().stream().sorted(Comparator.comparing(item -> item.getUnionUnit().getCode())).toList();
        for (int index = 0; index < reports.size(); index++) {
            MonthlyReport item = reports.get(index);
            writeRow(sheet.createRow(index + 3), new String[]{item.getUnionUnit().getCode(), item.getUnionUnit().getCompanyName(),
                    data.month().toString(), item.getPreparedBy(), date(item.getSubmittedAt()), item.getPlanNextMonth(),
                    item.getSupportRequest(), text(item.getStatus())}, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, reports.size() + 2), 0, headers.length - 1));
        setWidths(sheet, 14, 25, 15, 22, 20, 52, 52, 18);
    }

    private void createActivitySheet(XSSFWorkbook workbook, SummaryData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Chương trình");
        createTitle(sheet, "CHƯƠNG TRÌNH VÀ BÁO CÁO SAU CHƯƠNG TRÌNH", 17, styles);
        String[] headers = {"Mã chương trình", "CĐCS", "Tên chương trình", "Ngày", "Địa điểm", "PIC", "Trạng thái",
                "Mục tiêu", "Mời", "Tham gia", "Tỷ lệ TG", "Ngân sách", "Chi thực tế", "Chênh lệch", "Điểm hữu ích",
                "Kết quả / đề xuất", "Vấn đề follow-up", "Trạng thái follow-up"};
        createHeader(sheet.createRow(2), headers, styles);
        List<UnionActivity> activities = data.activities().stream().sorted(Comparator.comparing(UnionActivity::getEventDate).thenComparing(UnionActivity::getActivityCode)).toList();
        for (int index = 0; index < activities.size(); index++) {
            UnionActivity item = activities.get(index);
            writeRow(sheet.createRow(index + 3), new String[]{item.getActivityCode(), item.getUnionUnit().getCode(), item.getName(),
                    date(item.getEventDate()), item.getLocation(), item.getProgramPic(), text(item.getStatus()), item.getObjective(),
                    number(item.getInvitedCount()), number(item.getParticipantCount()), percent(item.getParticipantCount(), item.getInvitedCount()),
                    money(item.getPlannedBudget()), money(item.getActualCost()), money(subtract(item.getPlannedBudget(), item.getActualCost())),
                    number(item.getUsefulnessScore()), item.getOutputProposal(), item.getFollowUpIssue(), item.getFollowUpStatus()}, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, activities.size() + 2), 0, headers.length - 1));
        setWidths(sheet, 18, 14, 34, 15, 28, 22, 18, 44, 12, 12, 13, 18, 18, 18, 15, 50, 40, 22);
    }

    private void createWelfareSheet(XSSFWorkbook workbook, SummaryData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Chăm lo NLĐ");
        createTitle(sheet, "HỒ SƠ CHĂM LO NGƯỜI LAO ĐỘNG", 12, styles);
        String[] headers = {"Mã hồ sơ", "CĐCS", "Người thụ hưởng", "Loại chăm lo", "Chính sách", "Ngày phát sinh",
                "Hạn xử lý", "Trạng thái", "Số tiền", "Định mức", "Hồ sơ", "Biên nhận", "Ghi chú"};
        createHeader(sheet.createRow(2), headers, styles);
        List<WelfareRecord> records = data.welfare().stream().sorted(Comparator.comparing(WelfareRecord::getEventDate).thenComparing(WelfareRecord::getRecordCode)).toList();
        for (int index = 0; index < records.size(); index++) {
            WelfareRecord item = records.get(index);
            writeRow(sheet.createRow(index + 3), new String[]{item.getRecordCode(), item.getUnionUnit().getCode(), item.getBeneficiaryName(),
                    text(item.getWelfareType()), item.getPolicyName(), date(item.getEventDate()), date(item.getDeadline()), text(item.getStatus()),
                    money(item.getAmount()), money(item.getStandardAmount()), text(item.getDocumentStatus()), text(item.getReceiptStatus()), item.getNotes()}, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, records.size() + 2), 0, headers.length - 1));
        setWidths(sheet, 18, 14, 26, 18, 32, 16, 16, 18, 18, 18, 16, 16, 46);
    }

    private void createCaseSheet(XSSFWorkbook workbook, SummaryData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Kiến nghị NLĐ");
        createTitle(sheet, "KIẾN NGHỊ, PHẢN ÁNH CỦA NGƯỜI LAO ĐỘNG", 13, styles);
        String[] headers = {"Mã kiến nghị", "CĐCS", "Ngày nhận", "Người kiến nghị", "Nhóm", "Ưu tiên", "PIC", "Hạn xử lý",
                "Trạng thái", "Nội dung", "Kết quả", "Ngày phản hồi", "Nguyên nhân quá hạn", "Người duyệt"};
        createHeader(sheet.createRow(2), headers, styles);
        List<LaborCase> cases = data.cases().stream().sorted(Comparator.comparing(LaborCase::getReceivedDate).thenComparing(LaborCase::getCaseCode)).toList();
        for (int index = 0; index < cases.size(); index++) {
            LaborCase item = cases.get(index);
            writeRow(sheet.createRow(index + 3), new String[]{item.getCaseCode(), item.getUnionUnit().getCode(), date(item.getReceivedDate()),
                    item.getRequesterName(), item.getIssueGroup(), text(item.getSeverity()), item.getOwnerName(), date(item.getDeadline()),
                    text(item.getStatus()), item.getDescription(), item.getResultText(), date(item.getResponseDate()), item.getOverdueReason(), item.getApprovedBy()}, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, cases.size() + 2), 0, headers.length - 1));
        setWidths(sheet, 18, 14, 16, 26, 22, 15, 22, 16, 18, 54, 54, 16, 38, 22);
    }

    private void createFinanceSheet(XSSFWorkbook workbook, SummaryData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Tài chính");
        createTitle(sheet, "THU, CHI VÀ HỒ SƠ TÀI CHÍNH CÔNG ĐOÀN", 9, styles);
        String[] headers = {"Mã giao dịch", "CĐCS", "Ngày", "Loại", "Danh mục", "Số tiền", "Nội dung", "Số chứng từ", "Tình trạng chứng từ"};
        createHeader(sheet.createRow(2), headers, styles);
        List<FinanceEntry> entries = data.finance().stream().sorted(Comparator.comparing(FinanceEntry::getTransactionDate).thenComparing(FinanceEntry::getEntryCode)).toList();
        for (int index = 0; index < entries.size(); index++) {
            FinanceEntry item = entries.get(index);
            writeRow(sheet.createRow(index + 3), new String[]{item.getEntryCode(), item.getUnionUnit().getCode(), date(item.getTransactionDate()),
                    text(item.getEntryType()), item.getCategory(), money(item.getAmount()), item.getDescription(), item.getDocumentNumber(),
                    text(item.getDocumentStatus())}, styles.body);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(3, entries.size() + 2), 0, headers.length - 1));
        setWidths(sheet, 18, 14, 16, 15, 24, 18, 52, 20, 22);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 15);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setWrapText(true);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        border(header);

        CellStyle body = workbook.createCellStyle();
        body.setWrapText(true);
        body.setVerticalAlignment(VerticalAlignment.TOP);
        border(body);

        CellStyle note = workbook.createCellStyle();
        note.setWrapText(true);
        note.setVerticalAlignment(VerticalAlignment.CENTER);
        note.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        note.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return new Styles(title, header, body, note);
    }

    private void border(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void createTitle(Sheet sheet, String title, int lastColumn, Styles styles) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(28);
        setText(row, 0, title, styles.title);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, lastColumn));
    }

    private void createHeader(Row row, String[] headers, Styles styles) {
        row.setHeightInPoints(32);
        for (int index = 0; index < headers.length; index++) setText(row, index, headers[index], styles.header);
    }

    private int detailRow(Sheet sheet, int rowNumber, String group, String field, String value, String note, Styles styles) {
        Row row = sheet.createRow(rowNumber);
        setText(row, 0, group, styles.body);
        setText(row, 1, field, styles.body);
        setText(row, 2, value, styles.body);
        setText(row, 3, note, styles.body);
        return rowNumber + 1;
    }

    private void writeRow(Row row, String[] values, CellStyle style) {
        for (int index = 0; index < values.length; index++) setText(row, index, values[index], style);
    }

    private void setText(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(text(value));
        cell.setCellStyle(style);
    }

    private void setWidths(Sheet sheet, int... widths) {
        for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, Math.min(widths[index], 80) * 256);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Kỳ báo cáo phải có dạng yyyy-MM", exception);
        }
    }

    private boolean inScope(UnionUnit unit, Long scope) {
        return scope == null || unit.getId().equals(scope);
    }

    private boolean isInMonth(LocalDate date, YearMonth month) {
        return date != null && month.equals(YearMonth.from(date));
    }

    private BigDecimal sumFinance(List<FinanceEntry> entries, Long unitId, String type) {
        return entries.stream().filter(item -> item.getUnionUnit().getId().equals(unitId))
                .filter(item -> type.equals(text(item.getEntryType()))).map(FinanceEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String appendNote(String value) {
        return value == null || value.isBlank() ? "" : " · " + value;
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" · "));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String date(Object value) {
        return text(value);
    }

    private String time(Object value) {
        return value == null ? "" : String.valueOf(value).substring(0, Math.min(5, String.valueOf(value).length()));
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Có" : "Không";
    }

    private String number(Number value) {
        return value == null ? "" : value.toString();
    }

    private String money(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString() + " VNĐ";
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return (left == null ? BigDecimal.ZERO : left).subtract(right == null ? BigDecimal.ZERO : right);
    }

    private BigDecimal divide(BigDecimal amount, Integer divisor) {
        if (amount == null || divisor == null || divisor == 0) return BigDecimal.ZERO;
        return amount.divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP);
    }

    private String percent(Number numerator, Number denominator) {
        if (numerator == null || denominator == null || BigDecimal.valueOf(denominator.doubleValue()).compareTo(BigDecimal.ZERO) == 0) return "";
        return BigDecimal.valueOf(numerator.doubleValue() * 100 / denominator.doubleValue()).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private record Styles(CellStyle title, CellStyle header, CellStyle body, CellStyle note) {
    }

    private record SummaryData(YearMonth month, List<UnionUnit> units, List<Member> members, List<MonthlyReport> reports,
                               List<UnionActivity> activities, List<WelfareRecord> welfare, List<LaborCase> cases,
                               List<FinanceEntry> finance) {
    }
}

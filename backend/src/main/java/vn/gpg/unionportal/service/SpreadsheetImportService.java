package vn.gpg.unionportal.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.dto.ApiModels.*;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class SpreadsheetImportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 5_000;
    private static final int MAX_REPORTED_ERRORS = 100;
    private static final String DATA_SHEET = "Dữ liệu";
    private static final String GUIDE_SHEET = "Hướng dẫn";
    private static final String WELFARE_RULES_SHEET = "Quy tắc nhập liệu";
    private static final String WELFARE_POLICY_SHEET = "Danh mục chính sách";
    private static final String WELFARE_POLICY_RANGE = "activeWelfarePolicyChoices";
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d-M-uu"));

    private final UnionUnitRepository unitRepository;
    private final MemberRepository memberRepository;
    private final WelfareRecordRepository welfareRepository;
    private final WelfarePolicyRepository welfarePolicyRepository;
    private final LaborCaseRepository caseRepository;
    private final UnionActivityRepository activityRepository;
    private final FinanceEntryRepository financeRepository;
    private final PulseSurveyRepository surveyRepository;
    private final PulseSurveyResponseRepository responseRepository;
    private final MonthlyReportRepository reportRepository;
    private final AdminUserRepository userRepository;
    private final IntegrationRunRepository runRepository;
    private final EntityMapper mapper;
    private final UserAccountService userAccountService;
    private final CurrentUserService currentUser;
    private final Validator validator;
    private final RealtimeEventPublisher events;

    public SpreadsheetImportService(UnionUnitRepository unitRepository,
                                    MemberRepository memberRepository,
                                    WelfareRecordRepository welfareRepository,
                                    WelfarePolicyRepository welfarePolicyRepository,
                                    LaborCaseRepository caseRepository,
                                    UnionActivityRepository activityRepository,
                                    FinanceEntryRepository financeRepository,
                                    PulseSurveyRepository surveyRepository,
                                    PulseSurveyResponseRepository responseRepository,
                                    MonthlyReportRepository reportRepository,
                                    AdminUserRepository userRepository,
                                    IntegrationRunRepository runRepository,
                                    EntityMapper mapper,
                                    UserAccountService userAccountService,
                                    CurrentUserService currentUser,
                                    Validator validator,
                                    RealtimeEventPublisher events) {
        this.unitRepository = unitRepository;
        this.memberRepository = memberRepository;
        this.welfareRepository = welfareRepository;
        this.welfarePolicyRepository = welfarePolicyRepository;
        this.caseRepository = caseRepository;
        this.activityRepository = activityRepository;
        this.financeRepository = financeRepository;
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.runRepository = runRepository;
        this.mapper = mapper;
        this.userAccountService = userAccountService;
        this.currentUser = currentUser;
        this.validator = validator;
        this.events = events;
        ZipSecureFile.setMinInflateRatio(0.01d);
    }

    public byte[] createTemplate(String resourceName) {
        Resource resource = requireResource(resourceName);
        requireResourceAccess(resource);

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var styles = createStyles(workbook);
            List<WelfarePolicy> welfarePolicies = resource == Resource.WELFARE
                    ? createWelfarePolicySheet(workbook, styles)
                    : List.of();
            createDataSheet(workbook, resource, styles, !welfarePolicies.isEmpty());
            if (resource == Resource.WELFARE) {
                createWelfareRulesSheet(workbook, styles, !welfarePolicies.isEmpty());
                workbook.setSheetOrder(DATA_SHEET, 0);
                workbook.setSheetOrder(WELFARE_RULES_SHEET, 1);
                workbook.setSheetOrder(WELFARE_POLICY_SHEET, 2);
            }
            createGuideSheet(workbook, resource, styles);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo tệp Excel mẫu", exception);
        }
    }

    public byte[] exportReports(String monthValue, Long requestedUnitId) {
        Resource resource = Resource.REPORTS;
        requireResourceAccess(resource);
        YearMonth month = YearMonth.parse(monthValue);
        Long scopedUnitId = currentUser.scopedUnitId(requestedUnitId);
        var reports = reportRepository.findAll().stream()
                .filter(report -> YearMonth.from(report.getReportMonth()).equals(month))
                .filter(report -> scopedUnitId == null || report.getUnionUnit().getId().equals(scopedUnitId))
                .sorted(Comparator.comparing(report -> report.getUnionUnit().getCode()))
                .toList();

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var styles = createStyles(workbook);
            createDataSheet(workbook, resource, styles, false);
            createGuideSheet(workbook, resource, styles);
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            for (int rowIndex = 0; rowIndex < reports.size(); rowIndex++) {
                MonthlyReport report = reports.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < resource.columns.size(); columnIndex++) {
                    String value = switch (resource.columns.get(columnIndex).name()) {
                        case "unitCode" -> report.getUnionUnit().getCode();
                        case "month" -> YearMonth.from(report.getReportMonth()).toString();
                        case "preparedBy" -> report.getPreparedBy();
                        case "planNextMonth" -> report.getPlanNextMonth();
                        case "supportRequest" -> report.getSupportRequest();
                        case "status" -> report.getStatus().name();
                        default -> "";
                    };
                    Cell cell = row.createCell(columnIndex, CellType.STRING);
                    cell.setCellValue(value == null ? "" : value);
                    cell.setCellStyle(styles.body);
                }
            }
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(reports.size(), 1), 0, resource.columns.size() - 1));
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xuất báo cáo tháng", exception);
        }
    }

    public SpreadsheetImportResult importWorkbook(String resourceName, MultipartFile file) {
        Resource resource = requireResource(resourceName);
        requireResourceAccess(resource);
        var errors = new ArrayList<String>();
        int total = 0;
        int created = 0;
        int updated = 0;

        if (file == null || file.isEmpty()) {
            errors.add("Vui lòng chọn tệp Excel có dữ liệu.");
        } else if (file.getSize() > MAX_FILE_SIZE) {
            errors.add("Tệp Excel không được lớn hơn 10 MB.");
        } else {
            try (var input = file.getInputStream(); var workbook = WorkbookFactory.create(input)) {
                Sheet sheet = workbook.getSheet(DATA_SHEET);
                if (sheet == null && workbook.getNumberOfSheets() > 0) sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    errors.add("Tệp Excel không có sheet dữ liệu.");
                } else {
                    Map<String, Integer> headers = readHeaders(sheet.getRow(0), resource);
                    var missing = isBook1CaseLayout(resource, headers) ? List.<String>of() : resource.columns.stream()
                            .filter(Column::required)
                            .map(Column::name)
                            .filter(name -> !headers.containsKey(name.toLowerCase(Locale.ROOT)))
                            .toList();
                    if (!missing.isEmpty()) {
                        errors.add("Thiếu cột: " + String.join(", ", missing) + ". Hãy tải lại file mẫu của hệ thống.");
                    } else {
                        int lastRow = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);
                        for (int index = 1; index <= lastRow; index++) {
                            Row row = sheet.getRow(index);
                            if (isBlankRow(row, headers.values())) continue;
                            total++;
                            try {
                                boolean isCreated = importRow(resource, new RowValues(row, headers));
                                if (isCreated) created++;
                                else updated++;
                            } catch (Exception exception) {
                                addError(errors, "Dòng " + (index + 1) + ": " + safeMessage(exception));
                            }
                        }
                        if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
                            addError(errors, "Chỉ xử lý tối đa " + MAX_DATA_ROWS + " dòng dữ liệu trong một tệp.");
                        }
                        if (total == 0 && errors.isEmpty()) errors.add("Sheet dữ liệu chưa có dòng để nhập.");
                    }
                }
            } catch (IOException | RuntimeException exception) {
                errors.add("Không thể đọc tệp Excel. Hãy dùng định dạng .xlsx hợp lệ và không đặt mật khẩu.");
            }
        }

        int successful = created + updated;
        IntegrationRun run = saveRun(resource.integrationType, fileName(file), total, successful, errors);
        if (successful > 0) {
            events.changed(resource.path, "BULK_IMPORTED", null, null);
        }
        return new SpreadsheetImportResult(run, resource.path, created, updated, List.copyOf(errors));
    }

    public String templateFileName(String resourceName) {
        Resource resource = requireResource(resourceName);
        requireResourceAccess(resource);
        return resource.fileName;
    }

    /**
     * Technical name, Vietnamese header and cell kind for every "members" column, in template order.
     * This is the single source of truth for the member Excel schema — {@code MemberExcelService}
     * reads it instead of keeping its own duplicated header list.
     */
    public static List<ExportColumn> memberExportColumns() {
        return Resource.MEMBERS.columns.stream().map(column -> new ExportColumn(column.name(), column.header(), column.kind())).toList();
    }

    /** Column metadata handed to {@code MemberExcelService} so both exports render the same headers. */
    public record ExportColumn(String name, String header, String kind) {
    }

    private boolean importRow(Resource resource, RowValues row) {
        return switch (resource) {
            case UNITS -> importUnit(row);
            case MEMBERS -> importMember(row);
            case WELFARE -> importWelfare(row);
            case CASES -> importCase(row);
            case ACTIVITIES -> importActivity(row);
            case FINANCE -> importFinance(row);
            case SURVEYS -> importSurvey(row);
            case SURVEY_RESPONSES -> importSurveyResponse(row);
            case REPORTS -> importReport(row);
            case USERS -> importUser(row);
        };
    }

    private boolean importUnit(RowValues row) {
        String code = row.required("code");
        var existing = unitRepository.findByCodeIgnoreCase(code);
        var request = validate(new UnionUnitRequest(
                code, row.required("name"), row.required("companyName"), row.optional("location"),
                row.optional("chairperson"), row.date("termStart", false), row.date("termEnd", false),
                row.optional("decisionNumber"), row.enumValue("legalStatus", LegalStatus.class, true),
                row.optional("contactPerson")));
        if (request.termStart() != null && request.termEnd() != null && request.termEnd().isBefore(request.termStart())) {
            throw new IllegalArgumentException("termEnd không được trước termStart");
        }
        unitRepository.save(mapper.apply(existing.orElseGet(UnionUnit::new), request));
        return existing.isEmpty();
    }

    private boolean importMember(RowValues row) {
        String code = row.required("employeeCode");
        var existing = memberRepository.findByEmployeeCodeIgnoreCase(code);
        UnionUnit unit = scopedUnit(row.required("unitCode"), existing.map(Member::getUnionUnit).orElse(null));
        var request = validate(new MemberRequest(
                code, row.required("fullName"), unit.getId(), row.optional("jobTitle"), row.optional("workplace"),
                row.date("joinDate", false), row.enumValue("membershipStatus", MembershipStatus.class, true),
                row.enumValue("employmentStatus", EmploymentStatus.class, true), row.optional("email"), row.optional("phone"),
                row.optional("company"), row.optional("proposedUnionTitle"), row.optional("professionalTitle"),
                row.enumValue("gender", Gender.class, false), row.optional("ethnicity"), row.optional("placeOfBirth"),
                row.optional("nationalId"), Optional.ofNullable(row.bool("partyMember", false)).orElse(false),
                row.optional("education"), row.optional("specialization"), row.optional("politicalTheory"),
                row.optional("foreignLanguage"), row.date("startWorkDate", false), row.optional("currentResidence")));
        memberRepository.save(mapper.apply(existing.orElseGet(Member::new), request));
        return existing.isEmpty();
    }

    private boolean importWelfare(RowValues row) {
        String code = row.required("recordCode");
        var existing = welfareRepository.findByRecordCodeIgnoreCase(code);
        UnionUnit unit = scopedUnit(row.required("unitCode"), existing.map(WelfareRecord::getUnionUnit).orElse(null));
        LocalDate eventDate = row.date("eventDate", true);
        WelfarePolicy policy = selectedWelfarePolicy(row);
        BigDecimal amount = row.decimal("amount", false);
        if (amount == null && policy == null) {
            throw new IllegalArgumentException("amount không được để trống khi không chọn chính sách chăm lo");
        }
        var request = validate(new WelfareRequest(
                code, policy == null ? row.enumValue("welfareType", WelfareType.class, true) : policy.getWelfareType(),
                policy == null ? row.optional("policyName") : policy.getName(), unit.getId(),
                row.required("beneficiaryName"), eventDate,
                policy == null ? row.date("deadline", false) : eventDate.plusWeeks(policy.getProcessingWeeks()),
                row.enumValue("status", WorkStatus.class, true), amount == null ? policy.getSupportAmount() : amount,
                policy == null ? row.decimal("standardAmount", false) : policy.getSupportAmount(),
                row.enumValue("documentStatus", DocumentStatus.class, true),
                Optional.ofNullable(row.enumValue("receiptStatus", DocumentStatus.class, false)).orElse(DocumentStatus.INCOMPLETE),
                Optional.ofNullable(row.bool("hasImage", false)).orElse(false), row.optional("notes"),
                policy == null ? null : policy.getId()));
        welfareRepository.save(mapper.apply(existing.orElseGet(WelfareRecord::new), request));
        return existing.isEmpty();
    }

    private WelfarePolicy selectedWelfarePolicy(RowValues row) {
        String selection = row.optional("policyCode");
        if (selection == null) return null; // Keep files created with the former manual-policy template importable.
        String policyCode = selection.split("\\s+—\\s+", 2)[0].trim();
        WelfarePolicy policy = welfarePolicyRepository.findByCodeIgnoreCase(policyCode)
                .orElseThrow(() -> new IllegalArgumentException("không tìm thấy chính sách chăm lo có mã " + policyCode));
        if (!Boolean.TRUE.equals(policy.getActive())) {
            throw new IllegalArgumentException("chính sách chăm lo " + policyCode + " đã ngừng áp dụng");
        }
        return policy;
    }

    private boolean importCase(RowValues row) {
        LocalDate receivedDate = row.date("receivedDate", true);
        String employeeCode = row.optional("employeeCode");
        String code = Optional.ofNullable(row.optional("caseCode"))
                .orElseGet(() -> book1CaseCode(employeeCode, receivedDate, row.rowNumber()));
        var existing = caseRepository.findByCaseCodeIgnoreCase(code);
        UnionUnit unit = caseUnit(row, existing.map(LaborCase::getUnionUnit).orElse(null));
        if (!currentUser.isAdmin() && existing
                .map(LaborCase::getStatus)
                .filter(status -> status == CaseStatus.PENDING_APPROVAL || status == CaseStatus.CLOSED)
                .isPresent()) {
            throw new AccessDeniedException("USER không thể nhập đè vụ việc đang chờ duyệt hoặc đã đóng");
        }
        CaseStatus importedStatus = row.enumValue("status", CaseStatus.class, false);
        CaseStatus status = currentUser.isAdmin()
                ? Optional.ofNullable(importedStatus).orElseGet(() -> existing.map(LaborCase::getStatus).orElse(CaseStatus.NEW))
                : existing.map(LaborCase::getStatus).orElse(CaseStatus.NEW);
        String importedOwner = row.optional("ownerName");
        LocalDate importedDeadline = row.date("deadline", false);
        String ownerName = currentUser.isAdmin()
                ? Optional.ofNullable(importedOwner).orElseGet(() -> existing.map(LaborCase::getOwnerName).orElse(null))
                : existing.map(LaborCase::getOwnerName).orElse(null);
        LocalDate deadline = currentUser.isAdmin()
                ? Optional.ofNullable(importedDeadline).orElseGet(() -> existing.map(LaborCase::getDeadline).orElse(null))
                : existing.map(LaborCase::getDeadline).orElse(null);
        var request = validate(new LaborCaseRequest(
                code, receivedDate, unit.getId(),
                Optional.ofNullable(row.optional("requesterName")).orElse("Chưa cập nhật"),
                employeeCode, row.optional("jobTitle"), row.optional("workplace"),
                row.date("startWorkDate", false), row.date("leaveDate", false), row.optional("phone"),
                Optional.ofNullable(row.optional("source")).orElse("Excel xử lý vụ việc"),
                Optional.ofNullable(row.optional("issueGroup")).orElse("Yêu cầu người lao động"),
                Optional.ofNullable(row.enumValue("severity", CaseSeverity.class, false)).orElse(CaseSeverity.MEDIUM),
                ownerName, deadline, status,
                row.required("description"), Optional.ofNullable(row.integer("affectedPeople", false)).orElse(1),
                row.optional("attachmentNote"), row.optional("resultText"), row.date("responseDate", false),
                row.optional("overdueReason")));
        caseRepository.save(mapper.apply(existing.orElseGet(LaborCase::new), request));
        return existing.isEmpty();
    }

    private UnionUnit caseUnit(RowValues row, UnionUnit existingUnit) {
        if (existingUnit != null) {
            currentUser.requireUnitAccess(existingUnit.getId());
            String requestedCode = row.optional("unitCode");
            return requestedCode == null ? existingUnit : scopedUnit(requestedCode, existingUnit);
        }
        String unitCode = row.optional("unitCode");
        if (unitCode != null) return scopedUnit(unitCode, null);
        Long scopedUnitId = currentUser.scopedUnitId(null);
        if (scopedUnitId == null) {
            throw new IllegalArgumentException("ADMIN cần thêm cột Mã CĐCS khi nhập file xử lý vụ việc");
        }
        return unitRepository.findById(scopedUnitId)
                .orElseThrow(() -> new IllegalArgumentException("không tìm thấy CĐCS được gán cho USER"));
    }

    private String book1CaseCode(String employeeCode, LocalDate receivedDate, int rowNumber) {
        String employeePart = Optional.ofNullable(employeeCode)
                .map(value -> value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", ""))
                .filter(value -> !value.isBlank())
                .orElse("UNKNOWN");
        String code = "VV-" + employeePart + "-" + receivedDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-R" + rowNumber;
        return abbreviate(code, 40);
    }

    private boolean importActivity(RowValues row) {
        String code = row.required("activityCode");
        var existing = activityRepository.findByActivityCodeIgnoreCase(code);
        UnionUnit unit = scopedUnit(row.required("unitCode"), existing.map(UnionActivity::getUnionUnit).orElse(null));
        ActivityStatus status = row.enumValue("status", ActivityStatus.class, true);
        boolean reportCompleted = row.bool("reportCompleted", true);
        if (reportCompleted || status == ActivityStatus.COMPLETED) {
            throw new IllegalArgumentException("Excel chỉ dùng nhập kế hoạch; hãy nộp và đóng báo cáo tại mục Báo cáo chương trình");
        }
        var request = validate(new ActivityRequest(
                code, row.required("name"), unit.getId(), row.date("eventDate", true),
                null, null, null, status, row.optional("objective"),
                row.decimal("plannedBudget", true), row.decimal("actualCost", true),
                0, row.integer("participantCount", true), row.optional("participantList"), null,
                Optional.ofNullable(row.integer("checkInCount", false)).orElse(0), null, null, 0,
                row.decimal("usefulnessScore", false), row.optional("quickFeedback"), row.optional("issues"), null,
                null, null, null,
                false, Optional.ofNullable(row.enumValue("documentStatus", DocumentStatus.class, false)).orElse(DocumentStatus.INCOMPLETE),
                null, row.optional("followUpOwner"), row.date("followUpDeadline", false), null,
                row.optional("lessonsLearned")));
        activityRepository.save(mapper.apply(existing.orElseGet(UnionActivity::new), request));
        return existing.isEmpty();
    }

    private boolean importFinance(RowValues row) {
        String code = row.required("entryCode");
        var existing = financeRepository.findByEntryCodeIgnoreCase(code);
        UnionUnit unit = scopedUnit(row.required("unitCode"), existing.map(FinanceEntry::getUnionUnit).orElse(null));
        var request = validate(new FinanceRequest(
                code, unit.getId(), row.date("transactionDate", true),
                row.enumValue("entryType", FinanceEntryType.class, true), row.required("category"),
                row.decimal("amount", true), row.required("description"), row.optional("documentNumber"),
                row.enumValue("documentStatus", DocumentStatus.class, true)));
        financeRepository.save(mapper.apply(existing.orElseGet(FinanceEntry::new), request));
        return existing.isEmpty();
    }

    private boolean importSurvey(RowValues row) {
        String code = row.required("surveyCode");
        var existing = surveyRepository.findBySurveyCodeIgnoreCase(code);
        UnionUnit unit = scopedUnit(row.required("unitCode"), existing.map(PulseSurvey::getUnionUnit).orElse(null));
        var request = validate(new PulseSurveyRequest(
                code, row.required("title"), unit.getId(), row.required("questionText"),
                row.date("startDate", true), row.date("endDate", true),
                row.enumValue("status", SurveyStatus.class, true), row.integer("targetResponses", true)));
        surveyRepository.save(mapper.apply(existing.orElseGet(PulseSurvey::new), request));
        return existing.isEmpty();
    }

    private boolean importSurveyResponse(RowValues row) {
        String surveyCode = row.required("surveyCode");
        PulseSurvey survey = surveyRepository.findBySurveyCodeIgnoreCase(surveyCode)
                .orElseThrow(() -> new IllegalArgumentException("không tìm thấy khảo sát có mã " + surveyCode));
        currentUser.requireUnitAccess(survey.getUnionUnit().getId());
        int rating = row.integer("rating", true);
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("rating phải từ 1 đến 5");
        boolean anonymous = row.bool("anonymous", true);
        String respondentName = row.optional("respondentName");
        if (!anonymous && respondentName == null) throw new IllegalArgumentException("respondentName bắt buộc khi anonymous=FALSE");
        LocalDate submittedOn = row.date("submittedOn", true);
        if (submittedOn.isBefore(survey.getStartDate()) || submittedOn.isAfter(survey.getEndDate())) {
            throw new IllegalArgumentException("submittedOn phải nằm trong thời gian mở khảo sát");
        }
        var response = new PulseSurveyResponse();
        response.setSurvey(survey);
        response.setRating(rating);
        response.setNeedCategory(row.required("needCategory"));
        response.setSuggestion(row.optional("suggestion"));
        response.setAnonymous(anonymous);
        response.setRespondentName(anonymous ? null : respondentName);
        response.setSubmittedOn(submittedOn);
        responseRepository.save(response);
        return true;
    }

    private boolean importReport(RowValues row) {
        UnionUnit unit = scopedUnit(row.required("unitCode"), null);
        YearMonth month = row.month("month");
        var existing = reportRepository.findByUnionUnitIdAndReportMonth(unit.getId(), month.atDay(1));
        var request = validate(new MonthlyReportRequest(
                unit.getId(), month.toString(), row.required("preparedBy"), row.optional("planNextMonth"),
                row.optional("supportRequest"), row.enumValue("status", ReportStatus.class, true)));
        if (!currentUser.isAdmin()) {
            if (request.status() == ReportStatus.APPROVED) {
                throw new AccessDeniedException("USER không được nhập báo cáo ở trạng thái ADMIN đã duyệt");
            }
            if (existing.isPresent() && existing.get().getStatus() != ReportStatus.DRAFT) {
                throw new AccessDeniedException("Báo cáo đã nộp cho ADMIN không thể nhập lại từ Excel");
            }
        }
        reportRepository.save(mapper.apply(existing.orElseGet(MonthlyReport::new), request));
        return existing.isEmpty();
    }

    private boolean importUser(RowValues row) {
        String username = row.required("username");
        var existing = userRepository.findByUsernameIgnoreCase(username);
        String role = row.required("role").toUpperCase(Locale.ROOT);
        String unitCode = row.optional("unitCode");
        Long unitId = null;
        if ("USER".equals(role)) {
            if (unitCode == null) throw new IllegalArgumentException("unitCode bắt buộc với tài khoản USER");
            unitId = requireUnit(unitCode).getId();
        }
        String password = row.optional("password");
        var request = validate(new UserAccountRequest(
                username, row.required("fullName"), role, unitId, row.bool("active", true), password));
        if (existing.isPresent()) userAccountService.update(existing.get().getId(), request);
        else userAccountService.create(request);
        return existing.isEmpty();
    }

    private UnionUnit scopedUnit(String unitCode, UnionUnit existingUnit) {
        if (existingUnit != null) currentUser.requireUnitAccess(existingUnit.getId());
        UnionUnit unit = requireUnit(unitCode);
        currentUser.requireUnitAccess(unit.getId());
        return unit;
    }

    private UnionUnit requireUnit(String unitCode) {
        return unitRepository.findByCodeIgnoreCase(unitCode)
                .orElseThrow(() -> new IllegalArgumentException("không tìm thấy CĐCS có mã " + unitCode));
    }

    private <T> T validate(T request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(item -> item.getPropertyPath().toString()))
                    .map(this::violationMessage)
                    .reduce((left, right) -> left + "; " + right).orElse("dữ liệu không hợp lệ");
            throw new IllegalArgumentException(message);
        }
        return request;
    }

    private String violationMessage(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private Resource requireResource(String resourceName) {
        return Resource.fromPath(resourceName)
                .orElseThrow(() -> new IllegalArgumentException("Phân hệ Excel không hợp lệ: " + resourceName));
    }

    private void requireResourceAccess(Resource resource) {
        if (resource.adminOnly && !currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được nhập dữ liệu " + resource.title);
        }
    }

    private IntegrationRun saveRun(IntegrationType type, String fileName, int total, int successful, List<String> errors) {
        var run = new IntegrationRun();
        run.setIntegrationType(type);
        run.setStatus(errors.isEmpty() ? IntegrationStatus.COMPLETED
                : successful > 0 ? IntegrationStatus.PARTIAL : IntegrationStatus.FAILED);
        run.setFileName(fileName);
        run.setTotalRows(total);
        run.setSuccessfulRows(successful);
        run.setFailedRows(Math.max(total - successful, 0));
        run.setStartedBy(currentUser.username());
        run.setCompletedAt(Instant.now());
        run.setErrorSummary(errors.isEmpty() ? null : abbreviate(String.join(" | ", errors), 4000));
        return runRepository.save(run);
    }

    private Map<String, Integer> readHeaders(Row row, Resource resource) {
        if (row == null) throw new IllegalArgumentException("Sheet dữ liệu không có dòng tiêu đề");
        var headers = new LinkedHashMap<String, Integer>();
        for (Cell cell : row) {
            String value = cellText(cell);
            if (value == null) continue;
            String normalized = value.toLowerCase(Locale.ROOT);
            String key = resource == Resource.CASES ? caseHeaderAlias(value)
                    : resource == Resource.WELFARE ? welfareHeaderAlias(value) : null;
            if (key == null) key = resource.columns.stream()
                    .filter(column -> column.name.equalsIgnoreCase(value) || column.header().equalsIgnoreCase(value))
                    .map(column -> column.name.toLowerCase(Locale.ROOT))
                    .findFirst()
                    .orElse(normalized);
            if (headers.putIfAbsent(key, cell.getColumnIndex()) != null) {
                throw new IllegalArgumentException("Cột " + value + " bị lặp trong dòng tiêu đề");
            }
        }
        return headers;
    }

    private boolean isBook1CaseLayout(Resource resource, Map<String, Integer> headers) {
        return (resource == Resource.CASES
                && headers.containsKey("requestername")
                && headers.containsKey("receiveddate")
                && headers.containsKey("description"))
                || (resource == Resource.WELFARE
                && headers.containsKey("policyname")
                && headers.containsKey("welfaretype")
                && !headers.containsKey("policycode"));
    }

    private String caseHeaderAlias(String header) {
        String normalized = Normalizer.normalize(header, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return switch (normalized) {
            case "manv", "ma nv", "ma nhan vien" -> "employeecode";
            case "ho va ten", "ho ten" -> "requestername";
            case "chuc danh" -> "jobtitle";
            case "noi lam viec" -> "workplace";
            case "ngay vao lam" -> "startworkdate";
            case "ngay nghi" -> "leavedate";
            case "dien thoai" -> "phone";
            case "yeu cau" -> "description";
            case "ngay yeu cau" -> "receiveddate";
            case "ngay tra loi" -> "responsedate";
            default -> null;
        };
    }

    private String welfareHeaderAlias(String header) {
        String normalized = Normalizer.normalize(header, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return switch (normalized) {
            case "ten chinh sach / dinh muc ap dung", "ten chinh sach" -> "policyName";
            case "loai cham lo" -> "welfareType";
            case "han hoan tat" -> "deadline";
            case "so tien" -> "amount";
            case "dinh muc" -> "standardAmount";
            default -> null;
        };
    }

    private boolean isBlankRow(Row row, Collection<Integer> columns) {
        if (row == null) return true;
        return columns.stream().allMatch(index -> {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null || cell.getCellType() == CellType.BLANK) return true;
            return cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank();
        });
    }

    private String cellText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.FORMULA) {
            throw new IllegalArgumentException("không hỗ trợ ô công thức; hãy dán giá trị trước khi nhập");
        }
        String value = new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private String safeMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getMessage() == null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? "dữ liệu không hợp lệ" : abbreviate(message, 500);
    }

    private void addError(List<String> errors, String message) {
        if (errors.size() < MAX_REPORTED_ERRORS) errors.add(message);
        else if (errors.size() == MAX_REPORTED_ERRORS) errors.add("Còn thêm lỗi khác; hệ thống chỉ hiển thị 100 lỗi đầu tiên.");
    }

    private String fileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) return "unknown.xlsx";
        return abbreviate(file.getOriginalFilename().replace('\\', '_').replace('/', '_'), 255);
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void createDataSheet(XSSFWorkbook workbook, Resource resource, Styles styles, boolean hasWelfarePolicies) {
        XSSFSheet sheet = workbook.createSheet(DATA_SHEET);
        sheet.createFreezePane(0, 1);
        Row header = sheet.createRow(0);
        header.setHeightInPoints(28);
        for (int index = 0; index < resource.columns.size(); index++) {
            Column column = resource.columns.get(index);
            Cell cell = header.createCell(index);
            cell.setCellValue(column.header());
            cell.setCellStyle(styles.header);
            sheet.setColumnWidth(index, Math.min(Math.max(column.width, column.header().length() + 3), 55) * 256);
            if ("date".equals(column.kind)) sheet.setDefaultColumnStyle(index, styles.date);
            if ("month".equals(column.kind)) sheet.setDefaultColumnStyle(index, styles.month);
            if ("number".equals(column.kind)) sheet.setDefaultColumnStyle(index, styles.number);
            if (column.allowed.length > 0) addListValidation(sheet, index, column.allowed);
            if (resource == Resource.WELFARE && "policyCode".equals(column.name) && hasWelfarePolicies) {
                addFormulaListValidation(sheet, index, WELFARE_POLICY_RANGE,
                        "Chọn chính sách", "Chọn một chính sách từ danh mục đang áp dụng.");
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, resource.columns.size() - 1));
    }

    private List<WelfarePolicy> createWelfarePolicySheet(XSSFWorkbook workbook, Styles styles) {
        List<WelfarePolicy> policies = welfarePolicyRepository.findAll(Sort.by("sequenceNumber", "source", "id")).stream()
                .filter(policy -> Boolean.TRUE.equals(policy.getActive()))
                .toList();
        XSSFSheet sheet = workbook.createSheet(WELFARE_POLICY_SHEET);
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 3);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(26);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("DANH MỤC CHÍNH SÁCH CHĂM LO ĐANG ÁP DỤNG");
        titleCell.setCellStyle(styles.header);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row hint = sheet.createRow(1);
        Cell hintCell = hint.createCell(0);
        hintCell.setCellValue("Danh sách này do ADMIN ban hành. Ở sheet Dữ liệu, hãy chọn một giá trị trong cột Chính sách chăm lo.");
        hintCell.setCellStyle(styles.note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        String[] headers = {"Lựa chọn", "Mã chính sách", "Nội dung chính sách", "Loại chăm lo",
                "Mức hỗ trợ (VNĐ)", "Đối tượng / điều kiện", "Thời hạn xử lý (tuần)"};
        Row header = sheet.createRow(2);
        header.setHeightInPoints(30);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(styles.header);
        }

        for (int index = 0; index < policies.size(); index++) {
            WelfarePolicy policy = policies.get(index);
            Row row = sheet.createRow(index + 3);
            setCell(row, 0, policy.getCode() + " — " + policy.getName(), styles.body);
            setCell(row, 1, policy.getCode(), styles.body);
            setCell(row, 2, policy.getName(), styles.body);
            setCell(row, 3, policy.getWelfareType().name(), styles.body);
            Cell amount = row.createCell(4, CellType.NUMERIC);
            amount.setCellValue(policy.getSupportAmount().doubleValue());
            amount.setCellStyle(styles.number);
            setCell(row, 5, policy.getEligibilityNotes(), styles.body);
            Cell processingWeeks = row.createCell(6, CellType.NUMERIC);
            processingWeeks.setCellValue(policy.getProcessingWeeks());
            processingWeeks.setCellStyle(styles.number);
        }
        int[] widths = {44, 20, 38, 20, 22, 48, 24};
        for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);

        if (!policies.isEmpty()) {
            Name namedRange = workbook.createName();
            namedRange.setNameName(WELFARE_POLICY_RANGE);
            namedRange.setRefersToFormula("'" + WELFARE_POLICY_SHEET + "'!$A$4:$A$" + (policies.size() + 3));
        }
        return policies;
    }

    private void createWelfareRulesSheet(XSSFWorkbook workbook, Styles styles, boolean hasWelfarePolicies) {
        Sheet sheet = workbook.createSheet(WELFARE_RULES_SHEET);
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 1);
        Row title = sheet.createRow(0);
        title.setHeightInPoints(28);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("QUY TẮC NHẬP EXCEL CHĂM LO");
        titleCell.setCellStyle(styles.header);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        String[][] rules = {
                {"1", "Chọn chính sách", hasWelfarePolicies
                        ? "Bắt buộc chọn một chính sách trong dropdown ở cột Chính sách chăm lo. Danh mục chỉ gồm các chính sách đang áp dụng."
                        : "ADMIN cần tạo ít nhất một chính sách đang áp dụng trước khi tải lại mẫu; hiện chưa có lựa chọn để hiển thị."},
                {"2", "Áp dụng tự động", "Hệ thống tự lấy loại chăm lo, nội dung chính sách, định mức và hạn hoàn tất theo chính sách đã chọn."},
                {"3", "Số tiền thực tế", "Có thể để trống để dùng đúng mức hỗ trợ của chính sách. Nếu nhập số khác, cần ghi rõ lý do tại cột Ghi chú."},
                {"4", "Ngày và số", "Ngày theo yyyy-MM-dd. Số tiền chỉ nhập số, có thể dùng dấu phẩy phân cách hàng nghìn; không nhập công thức."},
                {"5", "Cập nhật hồ sơ", "Mã hồ sơ là khóa cập nhật: trùng mã sẽ cập nhật hồ sơ có sẵn. Không đổi tên cột trong sheet Dữ liệu."},
                {"6", "Trạng thái và chứng từ", "Chọn trạng thái, tình trạng hồ sơ và biên nhận bằng dropdown. Tệp đính kèm được tải lên sau khi nhập hồ sơ."}
        };
        String[] headers = {"STT", "Quy tắc", "Hướng dẫn"};
        Row header = sheet.createRow(2);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(styles.header);
        }
        for (int index = 0; index < rules.length; index++) {
            Row row = sheet.createRow(index + 3);
            for (int column = 0; column < rules[index].length; column++) setCell(row, column, rules[index][column], styles.body);
        }
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 26 * 256);
        sheet.setColumnWidth(2, 100 * 256);
    }

    private void createGuideSheet(XSSFWorkbook workbook, Resource resource, Styles styles) {
        Sheet sheet = workbook.createSheet(GUIDE_SHEET);
        sheet.createFreezePane(0, 1);
        Row header = sheet.createRow(0);
        String[] labels = {"Tên cột trong Excel", "Mã kỹ thuật", "Nội dung", "Bắt buộc", "Định dạng / giá trị hợp lệ"};
        for (int index = 0; index < labels.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(labels[index]);
            cell.setCellStyle(styles.header);
        }
        for (int index = 0; index < resource.columns.size(); index++) {
            Column column = resource.columns.get(index);
            Row row = sheet.createRow(index + 1);
            row.createCell(0).setCellValue(column.header());
            row.createCell(1).setCellValue(column.name);
            row.createCell(2).setCellValue(column.description);
            row.createCell(3).setCellValue(column.required ? "Có" : "Không");
            row.createCell(4).setCellValue(column.guide());
            for (Cell cell : row) cell.setCellStyle(styles.body);
        }
        Row note = sheet.createRow(resource.columns.size() + 3);
        note.createCell(0).setCellValue("Lưu ý");
        note.getCell(0).setCellStyle(styles.noteTitle);
        note.createCell(1).setCellValue("Không đổi tên cột tiếng Việt ở sheet Dữ liệu; ngày dùng yyyy-MM-dd; không dùng công thức. Mã đã tồn tại sẽ được cập nhật. USER chỉ được nhập dữ liệu thuộc CĐCS được gán.");
        note.getCell(1).setCellStyle(styles.note);
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 48 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.setColumnWidth(4, 60 * 256);
    }

    private void addListValidation(XSSFSheet sheet, int columnIndex, String[] values) {
        var helper = new XSSFDataValidationHelper(sheet);
        var constraint = helper.createExplicitListConstraint(values);
        var addresses = new CellRangeAddressList(1, MAX_DATA_ROWS, columnIndex, columnIndex);
        var validation = helper.createValidation(constraint, addresses);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Giá trị không hợp lệ", "Chọn một giá trị trong danh sách.");
        sheet.addValidationData(validation);
    }

    private void addFormulaListValidation(XSSFSheet sheet, int columnIndex, String formula,
                                          String title, String message) {
        var helper = new XSSFDataValidationHelper(sheet);
        var constraint = helper.createFormulaListConstraint(formula);
        var addresses = new CellRangeAddressList(1, MAX_DATA_ROWS, columnIndex, columnIndex);
        var validation = helper.createValidation(constraint, addresses);
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
    }

    private void setCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex, CellType.STRING);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setBorderBottom(BorderStyle.THIN);

        CellStyle body = workbook.createCellStyle();
        body.setWrapText(true);
        body.setVerticalAlignment(VerticalAlignment.TOP);
        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        CellStyle month = workbook.createCellStyle();
        month.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm"));
        CellStyle number = workbook.createCellStyle();
        number.setDataFormat(workbook.createDataFormat().getFormat("#,##0.##"));

        Font noteTitleFont = workbook.createFont();
        noteTitleFont.setBold(true);
        noteTitleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle noteTitle = workbook.createCellStyle();
        noteTitle.setFont(noteTitleFont);
        CellStyle note = workbook.createCellStyle();
        note.setWrapText(true);
        note.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        note.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return new Styles(header, body, date, month, number, noteTitle, note);
    }

    private record Styles(CellStyle header, CellStyle body, CellStyle date, CellStyle month,
                          CellStyle number, CellStyle noteTitle, CellStyle note) {
    }

    private record Column(String name, String description, boolean required, String kind, int width, String... allowed) {
        String header() {
            int end = description.length();
            for (char separator : new char[]{',', ';', ':'}) {
                int index = description.indexOf(separator);
                if (index >= 0) end = Math.min(end, index);
            }
            return description.substring(0, end).trim();
        }

        String guide() {
            if (allowed.length > 0) return String.join(" | ", allowed);
            return switch (kind) {
                case "date" -> "yyyy-MM-dd";
                case "month" -> "yyyy-MM";
                case "number" -> "Số";
                default -> "Văn bản";
            };
        }
    }

    private enum Resource {
        UNITS("units", "CĐCS", "mau-cdcs.xlsx", IntegrationType.UNITS_IMPORT, true, List.of(
                c("code", "Mã CĐCS, khóa cập nhật", true, 18), c("name", "Tên CĐCS", true, 30),
                c("companyName", "Tên công ty", true, 32), c("location", "Địa điểm", false, 26),
                c("chairperson", "Chủ tịch CĐCS", false, 24), d("termStart", "Ngày bắt đầu nhiệm kỳ", false),
                d("termEnd", "Ngày kết thúc nhiệm kỳ", false), c("decisionNumber", "Số quyết định", false, 20),
                e("legalStatus", "Tình trạng pháp lý", true, "ACTIVE", "INACTIVE"),
                c("contactPerson", "Đầu mối liên hệ", false, 24))),
        MEMBERS("members", "đoàn viên", "mau-doan-vien.xlsx", IntegrationType.MEMBERS_IMPORT, false, List.of(
                c("employeeCode", "Mã nhân viên, khóa cập nhật", true, 20), c("fullName", "Tên nhân viên", true, 28),
                c("unitCode", "Mã CĐCS", true, 18), c("company", "Công ty", false, 28),
                c("workplace", "Nơi làm việc", false, 26), c("proposedUnionTitle", "Đề xuất chức vụ Công đoàn", false, 28),
                c("professionalTitle", "Chức vụ chuyên môn", false, 24), c("jobTitle", "Chức danh", false, 22),
                e("gender", "Giới tính", false, "MALE", "FEMALE"), c("ethnicity", "Tên dân tộc", false, 18),
                c("placeOfBirth", "Nơi sinh", false, 26), c("nationalId", "CCCD", false, 18),
                e("partyMember", "Đảng viên", false, "TRUE", "FALSE"), c("education", "Học vấn", false, 20),
                c("specialization", "Chuyên môn", false, 26), c("politicalTheory", "Chính trị", false, 20),
                c("foreignLanguage", "Ngoại ngữ", false, 20), c("phone", "ĐT di động", false, 18),
                d("joinDate", "Ngày gia nhập công đoàn", false), d("startWorkDate", "Ngày vào làm", false),
                c("email", "Email", false, 28), c("currentResidence", "Nơi ở hiện tại", false, 32),
                e("membershipStatus", "Tình trạng công đoàn", true, "MEMBER", "NOT_JOINED", "LEFT"),
                e("employmentStatus", "Trạng thái nhân sự", true, "ACTIVE", "INACTIVE"))),
        WELFARE("welfare", "chăm lo", "mau-cham-lo.xlsx", IntegrationType.WELFARE_IMPORT, false, List.of(
                c("recordCode", "Mã hồ sơ, khóa cập nhật", true, 20),
                c("policyCode", "Chính sách chăm lo, chọn từ danh mục", true, 44), c("unitCode", "Mã CĐCS", true, 18), c("beneficiaryName", "Người thụ hưởng", true, 28),
                d("eventDate", "Ngày sự kiện", true), e("status", "Trạng thái xử lý", true, "NEW", "PENDING_APPROVAL", "IN_PROGRESS", "COMPLETED", "CANCELLED"),
                n("amount", "Số tiền thực tế, để trống dùng định mức", false), e("documentStatus", "Tình trạng hồ sơ", true, "COMPLETE", "INCOMPLETE", "NOT_REQUIRED"),
                e("receiptStatus", "Tình trạng biên nhận", false, "COMPLETE", "INCOMPLETE", "NOT_REQUIRED"), e("hasImage", "Có hình ảnh", false, "TRUE", "FALSE"),
                c("notes", "Ghi chú", false, 42))),
        CASES("cases", "vụ việc", "mau-vu-viec.xlsx", IntegrationType.CASES_IMPORT, false, List.of(
                c("caseCode", "Mã vụ việc, khóa cập nhật", true, 20), d("receivedDate", "Ngày tiếp nhận", true),
                c("unitCode", "Mã CĐCS", true, 18), c("requesterName", "Người gửi", false, 26), c("source", "Kênh tiếp nhận", false, 22), c("issueGroup", "Nhóm vấn đề", true, 24),
                e("severity", "Mức độ", true, "LOW", "MEDIUM", "HIGH", "CRITICAL"), c("ownerName", "PIC do ADMIN giao", false, 24),
                d("deadline", "Deadline do ADMIN giao", false), e("status", "Trạng thái", false, "NEW", "VERIFYING", "CLASSIFYING", "ASSIGNED", "IN_PROGRESS", "WAITING_RESPONSE", "PENDING_APPROVAL", "CLOSED"),
                c("description", "Mô tả", true, 44), n("affectedPeople", "Số NLĐ ảnh hưởng", true),
                c("attachmentNote", "Tài liệu đính kèm / liên kết", false, 38), c("resultText", "Kết quả / phản hồi", false, 44), c("overdueReason", "Lý do quá hạn / ETA mới", false, 38),
                c("employeeCode", "MãNV", false, 18), c("jobTitle", "Chức danh", false, 24), c("workplace", "Noi làm việc", false, 30),
                d("startWorkDate", "Ngày vào làm", false), d("leaveDate", "Ngày nghỉ", false), c("phone", "Điện thoại", false, 18),
                d("responseDate", "Ngày trả lời", false))),
        ACTIVITIES("activities", "hoạt động", "mau-hoat-dong.xlsx", IntegrationType.ACTIVITIES_IMPORT, false, List.of(
                c("activityCode", "Mã hoạt động, khóa cập nhật", true, 20), c("name", "Tên chương trình", true, 32),
                c("unitCode", "Mã CĐCS", true, 18), d("eventDate", "Ngày tổ chức", true),
                e("status", "Trạng thái", true, "PLANNED", "APPROVED", "IN_PROGRESS", "COMPLETED", "CANCELLED"),
                c("objective", "Mục tiêu", false, 38), n("plannedBudget", "Ngân sách dự kiến", true),
                n("actualCost", "Chi phí thực tế", true), n("participantCount", "Số người tham dự", true),
                c("participantList", "Danh sách tham dự", false, 42), n("checkInCount", "Số người check-in", false),
                n("usefulnessScore", "Điểm hữu ích 0-5", false), c("quickFeedback", "Phản hồi nhanh", false, 42), c("issues", "Vấn đề phát sinh", false, 42),
                e("reportCompleted", "Đã có báo cáo", true, "TRUE", "FALSE"), e("documentStatus", "Tình trạng chứng từ", false, "COMPLETE", "INCOMPLETE", "NOT_REQUIRED"),
                c("followUpOwner", "Người follow-up", false, 24), d("followUpDeadline", "Hạn follow-up", false), c("lessonsLearned", "Bài học", false, 42))),
        FINANCE("finance", "tài chính nội bộ", "mau-tai-chinh-noi-bo.xlsx", IntegrationType.FINANCE_EXCEL_IMPORT, false, List.of(
                c("entryCode", "Mã phiếu, khóa cập nhật", true, 20), c("unitCode", "Mã CĐCS", true, 18),
                d("transactionDate", "Ngày giao dịch nội bộ", true), e("entryType", "Loại phiếu", true, "INCOME", "EXPENSE", "ADVANCE"),
                c("category", "Nhóm nghiệp vụ", true, 24), n("amount", "Số tiền", true),
                c("description", "Nội dung", true, 42), c("documentNumber", "Số chứng từ", false, 20),
                e("documentStatus", "Tình trạng chứng từ", true, "COMPLETE", "INCOMPLETE", "NOT_REQUIRED"))),
        SURVEYS("surveys", "khảo sát", "mau-khao-sat.xlsx", IntegrationType.SURVEYS_IMPORT, false, List.of(
                c("surveyCode", "Mã khảo sát, khóa cập nhật", true, 20), c("title", "Tên chiến dịch", true, 34),
                c("unitCode", "Mã CĐCS", true, 18), c("questionText", "Câu hỏi chính", true, 46),
                d("startDate", "Ngày bắt đầu", true), d("endDate", "Ngày kết thúc", true),
                e("status", "Trạng thái", true, "DRAFT", "ACTIVE", "CLOSED"), n("targetResponses", "Mục tiêu phản hồi", true))),
        SURVEY_RESPONSES("survey-responses", "phản hồi khảo sát", "mau-phan-hoi-khao-sat.xlsx", IntegrationType.SURVEY_RESPONSES_IMPORT, false, List.of(
                c("surveyCode", "Mã khảo sát", true, 20), n("rating", "Điểm đánh giá 1-5", true),
                c("needCategory", "Nhu cầu ưu tiên", true, 28), c("suggestion", "Ý kiến / đề xuất", false, 46),
                e("anonymous", "Phản hồi ẩn danh", true, "TRUE", "FALSE"),
                c("respondentName", "Họ tên; bắt buộc nếu không ẩn danh", false, 28), d("submittedOn", "Ngày gửi phản hồi", true))),
        REPORTS("reports", "báo cáo tháng", "mau-bao-cao-thang.xlsx", IntegrationType.REPORTS_IMPORT, false, List.of(
                c("unitCode", "Mã CĐCS, cùng tháng là khóa cập nhật", true, 18), m("month", "Kỳ báo cáo", true),
                c("preparedBy", "Người lập", true, 26), c("planNextMonth", "Kế hoạch tháng tới", false, 46),
                c("supportRequest", "Đề xuất / yêu cầu hỗ trợ", false, 46),
                e("status", "Trạng thái; USER chỉ dùng DRAFT hoặc SUBMITTED", true, "DRAFT", "SUBMITTED", "APPROVED"))),
        USERS("users", "tài khoản", "mau-tai-khoan.xlsx", IntegrationType.USERS_IMPORT, true, List.of(
                c("username", "Tên đăng nhập, khóa cập nhật", true, 24), c("fullName", "Họ tên", true, 28),
                e("role", "Vai trò", true, "ADMIN", "USER"), c("unitCode", "Mã CĐCS; bắt buộc với USER", false, 18),
                e("active", "Trạng thái hoạt động", true, "TRUE", "FALSE"),
                c("password", "Mật khẩu: bắt buộc khi tạo, để trống khi cập nhật nếu giữ nguyên", false, 34)));

        private final String path;
        private final String title;
        private final String fileName;
        private final IntegrationType integrationType;
        private final boolean adminOnly;
        private final List<Column> columns;

        Resource(String path, String title, String fileName, IntegrationType integrationType, boolean adminOnly, List<Column> columns) {
            this.path = path;
            this.title = title;
            this.fileName = fileName;
            this.integrationType = integrationType;
            this.adminOnly = adminOnly;
            this.columns = columns;
        }

        static Optional<Resource> fromPath(String value) {
            return Arrays.stream(values()).filter(item -> item.path.equalsIgnoreCase(value)).findFirst();
        }

        private static Column c(String name, String description, boolean required, int width) {
            return new Column(name, description, required, "text", width);
        }

        private static Column d(String name, String description, boolean required) {
            return new Column(name, description, required, "date", 17);
        }

        private static Column m(String name, String description, boolean required) {
            return new Column(name, description, required, "month", 15);
        }

        private static Column n(String name, String description, boolean required) {
            return new Column(name, description, required, "number", 18);
        }

        private static Column e(String name, String description, boolean required, String... allowed) {
            return new Column(name, description, required, "enum", Math.max(18, name.length() + 3), allowed);
        }
    }

    private final class RowValues {
        private final Row row;
        private final Map<String, Integer> headers;

        private RowValues(Row row, Map<String, Integer> headers) {
            this.row = row;
            this.headers = headers;
        }

        String required(String name) {
            String value = optional(name);
            if (value == null) throw new IllegalArgumentException(name + " không được để trống");
            return value;
        }

        String optional(String name) {
            return cellText(cell(name));
        }

        LocalDate date(String name, boolean required) {
            Cell cell = cell(name);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                if (required) throw new IllegalArgumentException(name + " không được để trống");
                return null;
            }
            if (cell.getCellType() == CellType.FORMULA) throw new IllegalArgumentException(name + " không hỗ trợ công thức");
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String value = required(name);
            for (DateTimeFormatter formatter : DATE_FORMATS) {
                try {
                    return LocalDate.parse(value, formatter);
                } catch (DateTimeParseException ignored) {
                    // Try the next supported format.
                }
            }
            throw new IllegalArgumentException(name + " phải theo định dạng yyyy-MM-dd hoặc d/M/yyyy");
        }

        int rowNumber() {
            return row.getRowNum() + 1;
        }

        YearMonth month(String name) {
            Cell cell = cell(name);
            if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return YearMonth.from(cell.getLocalDateTimeCellValue());
            }
            try {
                return YearMonth.parse(required(name));
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(name + " phải theo định dạng yyyy-MM");
            }
        }

        BigDecimal decimal(String name, boolean required) {
            Cell cell = cell(name);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                if (required) throw new IllegalArgumentException(name + " không được để trống");
                return null;
            }
            if (cell.getCellType() == CellType.FORMULA) throw new IllegalArgumentException(name + " không hỗ trợ công thức");
            try {
                if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
                return new BigDecimal(required(name).replace(",", ""));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(name + " phải là số hợp lệ");
            }
        }

        Integer integer(String name, boolean required) {
            BigDecimal value = decimal(name, required);
            if (value == null) return null;
            try {
                return value.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(name + " phải là số nguyên");
            }
        }

        Boolean bool(String name, boolean required) {
            String value = optional(name);
            if (value == null) {
                if (required) throw new IllegalArgumentException(name + " không được để trống");
                return null;
            }
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "TRUE", "1", "YES", "CÓ", "CO" -> true;
                case "FALSE", "0", "NO", "KHÔNG", "KHONG" -> false;
                default -> throw new IllegalArgumentException(name + " chỉ nhận TRUE hoặc FALSE");
            };
        }

        <T extends Enum<T>> T enumValue(String name, Class<T> type, boolean required) {
            String value = optional(name);
            if (value == null) {
                if (required) throw new IllegalArgumentException(name + " không được để trống");
                return null;
            }
            try {
                return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(name + " có giá trị không hợp lệ: " + value);
            }
        }

        private Cell cell(String name) {
            Integer index = headers.get(name.toLowerCase(Locale.ROOT));
            return index == null ? null : row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        }
    }
}

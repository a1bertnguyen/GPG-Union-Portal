package vn.gpg.unionportal.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.SpreadsheetImportResult;
import vn.gpg.unionportal.dto.ApiModels.WelfarePolicyRequest;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.IntegrationType;
import vn.gpg.unionportal.model.DomainEnums.WelfarePolicySource;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.IntegrationRun;
import vn.gpg.unionportal.model.WelfarePolicy;
import vn.gpg.unionportal.repository.IntegrationRunRepository;
import vn.gpg.unionportal.repository.WelfarePolicyRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

@Service
public class WelfarePolicyExcelService {
    private static final String RESOURCE = "welfare-policies";
    private static final String SHEET_NAME = "Quy Định Chế Độ Hỗ Trợ";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_ROWS = 5_000;
    private static final Sort SORT = Sort.by("sequenceNumber", "source", "id");

    private final WelfarePolicyRepository repository;
    private final WelfarePolicyService policyService;
    private final IntegrationRunRepository runRepository;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public WelfarePolicyExcelService(WelfarePolicyRepository repository,
                                     WelfarePolicyService policyService,
                                     IntegrationRunRepository runRepository,
                                     CurrentUserService currentUser,
                                     RealtimeEventPublisher events) {
        this.repository = repository;
        this.policyService = policyService;
        this.runRepository = runRepository;
        this.currentUser = currentUser;
        this.events = events;
    }

    public boolean supports(String resource) {
        return RESOURCE.equalsIgnoreCase(resource);
    }

    public String fileName() {
        return "bang-chinh-sach-cham-lo.xlsx";
    }

    public byte[] exportWorkbook() {
        List<WelfarePolicy> policies = repository.findAll(SORT);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 4);

            Row title = sheet.createRow(0);
            title.setHeightInPoints(28);
            setText(title, 0, "BẢNG QUY ĐỊNH CHẾ ĐỘ HỖ TRỢ PHÚC LỢI", styles.title());
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Row subtitle = sheet.createRow(1);
            setText(subtitle, 0, "Áp dụng cho Công đoàn và Công ty", styles.subtitle());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            Row header = sheet.createRow(3);
            header.setHeightInPoints(38);
            String[] headers = {"TT", "NỘI DUNG", "HỖ TRỢ\n(VNĐ)", "GHI CHÚ",
                    "THỜI HẠN XỬ LÝ\n(TUẦN)", "MÃ CHÍNH SÁCH", "LOẠI CHĂM LO", "TRẠNG THÁI"};
            for (int index = 0; index < headers.length; index++) setText(header, index, headers[index], styles.header());

            int rowIndex = 4;
            for (WelfarePolicySource source : List.of(WelfarePolicySource.UNION, WelfarePolicySource.COMPANY)) {
                Row section = sheet.createRow(rowIndex++);
                setText(section, 0, source == WelfarePolicySource.UNION ? "I. Công Đoàn Hỗ trợ:" : "II. Công ty hỗ trợ:", styles.section());
                sheet.addMergedRegion(new CellRangeAddress(section.getRowNum(), section.getRowNum(), 0, 7));
                for (WelfarePolicy policy : policies.stream().filter(item -> item.getSource() == source).toList()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.setHeightInPoints(34);
                    setNumber(row, 0, policy.getSequenceNumber(), styles.center());
                    setText(row, 1, policy.getName(), styles.body());
                    setNumber(row, 2, policy.getSupportAmount().doubleValue(), styles.money());
                    setText(row, 3, policy.getEligibilityNotes(), styles.body());
                    setNumber(row, 4, policy.getProcessingWeeks(), styles.center());
                    setText(row, 5, policy.getCode(), styles.center());
                    setText(row, 6, policy.getWelfareType().name(), styles.center());
                    setText(row, 7, Boolean.TRUE.equals(policy.getActive()) ? "ACTIVE" : "INACTIVE", styles.center());
                }
            }

            int[] widths = {8, 56, 20, 42, 20, 22, 20, 18};
            for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);
            sheet.setAutoFilter(new CellRangeAddress(3, 3, 0, 7));
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xuất file chính sách chăm lo", exception);
        }
    }

    @Transactional
    public SpreadsheetImportResult importWorkbook(MultipartFile file) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được nhập Excel chính sách chăm lo");
        }
        List<String> errors = new ArrayList<>();
        int total = 0;
        int created = 0;
        int updated = 0;

        if (file == null || file.isEmpty()) {
            errors.add("Vui lòng chọn tệp Excel có dữ liệu.");
        } else if (file.getSize() > MAX_FILE_SIZE) {
            errors.add("Tệp Excel không được lớn hơn 10 MB.");
        } else {
            try (var input = file.getInputStream(); var workbook = WorkbookFactory.create(input)) {
                Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
                if (sheet == null) {
                    errors.add("Tệp Excel không có sheet dữ liệu.");
                } else {
                    HeaderColumns columns = findHeaders(sheet);
                    WelfarePolicySource source = null;
                    Map<String, Integer> occurrences = new HashMap<>();
                    int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS);
                    for (int index = columns.rowIndex() + 1; index <= lastRow; index++) {
                        Row row = sheet.getRow(index);
                        String marker = normalized(joinCellText(row));
                        if (marker.contains("cong doan ho tro")) {
                            source = WelfarePolicySource.UNION;
                            continue;
                        }
                        if (marker.contains("cong ty ho tro")) {
                            source = WelfarePolicySource.COMPANY;
                            continue;
                        }
                        Integer sequence = integerCell(row, columns.sequence(), false);
                        String name = textCell(row, columns.name());
                        if (sequence == null && name == null) continue;
                        total++;
                        try {
                            if (source == null) throw new IllegalArgumentException("chưa xác định nhóm Công đoàn/Công ty hỗ trợ");
                            if (sequence == null || sequence < 1) throw new IllegalArgumentException("TT phải là số nguyên dương");
                            if (name == null) throw new IllegalArgumentException("NỘI DUNG không được để trống");
                            BigDecimal amount = decimalCell(row, columns.amount());
                            int weeks = Optional.ofNullable(integerCell(row, columns.weeks(), false)).orElse(1);
                            String occurrenceKey = source + ":" + sequence;
                            int occurrence = occurrences.merge(occurrenceKey, 1, Integer::sum);
                            String code = textCell(row, columns.code());
                            if (code == null) code = generatedCode(source, sequence, occurrence);
                            WelfareType type = Optional.ofNullable(textCell(row, columns.welfareType()))
                                    .map(this::parseWelfareType).orElseGet(() -> inferWelfareType(name));
                            boolean active = Optional.ofNullable(textCell(row, columns.active()))
                                    .map(this::parseActive).orElse(true);
                            var request = new WelfarePolicyRequest(code, source, sequence, type, name, amount,
                                    textCell(row, columns.notes()), weeks, active);
                            validate(request);
                            Optional<WelfarePolicy> existing = repository.findByCodeIgnoreCase(code);
                            repository.save(policyService.apply(existing.orElseGet(WelfarePolicy::new), request));
                            if (existing.isEmpty()) created++; else updated++;
                        } catch (Exception exception) {
                            addError(errors, "Dòng " + (index + 1) + ": " + safeMessage(exception));
                        }
                    }
                    if (sheet.getLastRowNum() > MAX_ROWS) addError(errors, "Chỉ xử lý tối đa " + MAX_ROWS + " dòng dữ liệu trong một tệp.");
                    if (total == 0 && errors.isEmpty()) errors.add("File chưa có dòng chính sách để nhập.");
                }
            } catch (IOException | RuntimeException exception) {
                errors.add("Không thể đọc tệp Excel. Hãy dùng file .xlsx hợp lệ, không đặt mật khẩu và giữ dòng tiêu đề.");
            }
        }

        int successful = created + updated;
        IntegrationRun run = saveRun(file, total, successful, errors);
        if (successful > 0) events.changed(RESOURCE, "BULK_IMPORTED", null, null);
        return new SpreadsheetImportResult(run, RESOURCE, created, updated, List.copyOf(errors));
    }

    private HeaderColumns findHeaders(Sheet sheet) {
        int limit = Math.min(sheet.getLastRowNum(), 15);
        for (int rowIndex = 0; rowIndex <= limit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : row) {
                String value = normalized(textCell(row, cell.getColumnIndex()));
                if (value != null) headers.put(value, cell.getColumnIndex());
            }
            Integer sequence = findHeader(headers, "tt");
            Integer name = findHeader(headers, "noi dung");
            Integer amount = findHeader(headers, "ho tro");
            if (sequence != null && name != null && amount != null) {
                return new HeaderColumns(rowIndex, sequence, name, amount,
                        findHeader(headers, "ghi chu"), findHeader(headers, "thoi han xu ly"),
                        findHeader(headers, "ma chinh sach"), findHeader(headers, "loai cham lo"),
                        findHeader(headers, "trang thai"));
            }
        }
        throw new IllegalArgumentException("Không tìm thấy dòng tiêu đề TT / NỘI DUNG / HỖ TRỢ");
    }

    private Integer findHeader(Map<String, Integer> headers, String prefix) {
        return headers.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private void validate(WelfarePolicyRequest request) {
        if (request.code().isBlank() || request.code().length() > 40) throw new IllegalArgumentException("mã chính sách tối đa 40 ký tự");
        if (request.name().isBlank() || request.name().length() > 180) throw new IllegalArgumentException("nội dung tối đa 180 ký tự");
        if (request.supportAmount().signum() < 0) throw new IllegalArgumentException("mức hỗ trợ không được âm");
        if (request.eligibilityNotes() != null && request.eligibilityNotes().length() > 1000) throw new IllegalArgumentException("ghi chú tối đa 1000 ký tự");
        if (request.processingWeeks() < 1 || request.processingWeeks() > 8) throw new IllegalArgumentException("thời hạn xử lý phải từ 1 đến 8 tuần");
    }

    private WelfareType parseWelfareType(String value) {
        try {
            return WelfareType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return inferWelfareType(value);
        }
    }

    private WelfareType inferWelfareType(String value) {
        String text = normalized(value);
        if (text.contains("ma chay") || text.contains("hieu")) return WelfareType.FUNERAL;
        if (text.contains("dam cuoi") || text.contains("hy")) return WelfareType.WEDDING;
        if (text.contains("sinh nhat")) return WelfareType.BIRTHDAY;
        if (text.contains("nam vien") || text.contains("tham hoi")) return WelfareType.VISIT;
        if (text.contains("sinh con")) return WelfareType.CHILDBIRTH;
        return WelfareType.HARDSHIP;
    }

    private boolean parseActive(String value) {
        return switch (normalized(value)) {
            case "active", "true", "1", "dang ap dung", "co" -> true;
            case "inactive", "false", "0", "ngung ap dung", "khong" -> false;
            default -> throw new IllegalArgumentException("TRẠNG THÁI chỉ nhận ACTIVE hoặc INACTIVE");
        };
    }

    private String generatedCode(WelfarePolicySource source, int sequence, int occurrence) {
        return "%s-%02d-%02d".formatted(source == WelfarePolicySource.UNION ? "CD" : "CT", sequence, occurrence);
    }

    private Integer integerCell(Row row, Integer index, boolean required) {
        if (index == null) {
            if (required) throw new IllegalArgumentException("thiếu cột số bắt buộc");
            return null;
        }
        Cell cell = row == null ? null : row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            if (required) throw new IllegalArgumentException("ô số không được để trống");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) throw new IllegalArgumentException("không hỗ trợ ô công thức");
        try {
            if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue()).intValueExact();
            return new BigDecimal(textCell(row, index).replaceAll("[^0-9-]", "")).intValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("giá trị phải là số nguyên");
        }
    }

    private BigDecimal decimalCell(Row row, Integer index) {
        if (index == null) throw new IllegalArgumentException("thiếu cột HỖ TRỢ");
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) throw new IllegalArgumentException("HỖ TRỢ không được để trống");
        if (cell.getCellType() == CellType.FORMULA) throw new IllegalArgumentException("không hỗ trợ ô công thức");
        try {
            if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return new BigDecimal(textCell(row, index).replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("HỖ TRỢ phải là số hợp lệ");
        }
    }

    private String joinCellText(Row row) {
        if (row == null) return "";
        StringBuilder value = new StringBuilder();
        for (Cell cell : row) {
            String text = textCell(row, cell.getColumnIndex());
            if (text != null) value.append(' ').append(text);
        }
        return value.toString();
    }

    private String textCell(Row row, Integer index) {
        if (row == null || index == null) return null;
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.FORMULA) throw new IllegalArgumentException("không hỗ trợ ô công thức; hãy dán giá trị trước khi nhập");
        String value = new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private String normalized(String value) {
        if (value == null) return null;
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private IntegrationRun saveRun(MultipartFile file, int total, int successful, List<String> errors) {
        var run = new IntegrationRun();
        run.setIntegrationType(IntegrationType.WELFARE_POLICIES_IMPORT);
        run.setStatus(errors.isEmpty() ? IntegrationStatus.COMPLETED
                : successful > 0 ? IntegrationStatus.PARTIAL : IntegrationStatus.FAILED);
        run.setFileName(safeFileName(file));
        run.setTotalRows(total);
        run.setSuccessfulRows(successful);
        run.setFailedRows(Math.max(total - successful, 0));
        run.setStartedBy(currentUser.username());
        run.setCompletedAt(Instant.now());
        run.setErrorSummary(errors.isEmpty() ? null : abbreviate(String.join(" | ", errors), 4000));
        return runRepository.save(run);
    }

    private String safeFileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) return "unknown.xlsx";
        return abbreviate(file.getOriginalFilename().replace('\\', '_').replace('/', '_'), 255);
    }

    private void addError(List<String> errors, String message) {
        if (errors.size() < 100) errors.add(message);
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "dữ liệu không hợp lệ" : abbreviate(exception.getMessage(), 500);
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        Font subtitleFont = workbook.createFont();
        subtitleFont.setItalic(true);
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle subtitle = workbook.createCellStyle();
        subtitle.setFont(subtitleFont);
        subtitle.setAlignment(HorizontalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = bordered(workbook);
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);

        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle section = bordered(workbook);
        section.setFont(sectionFont);
        section.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        section.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle body = bordered(workbook);
        body.setVerticalAlignment(VerticalAlignment.CENTER);
        body.setWrapText(true);
        CellStyle center = bordered(workbook);
        center.setAlignment(HorizontalAlignment.CENTER);
        center.setVerticalAlignment(VerticalAlignment.CENTER);
        center.setWrapText(true);
        CellStyle money = bordered(workbook);
        money.setAlignment(HorizontalAlignment.RIGHT);
        money.setVerticalAlignment(VerticalAlignment.CENTER);
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return new Styles(title, subtitle, header, section, body, center, money);
    }

    private CellStyle bordered(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setBottomBorderColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setLeftBorderColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setRightBorderColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        return style;
    }

    private void setText(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.STRING);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int index, Number value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.NUMERIC);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private record HeaderColumns(int rowIndex, int sequence, int name, int amount, Integer notes,
                                 Integer weeks, Integer code, Integer welfareType, Integer active) {
    }

    private record Styles(CellStyle title, CellStyle subtitle, CellStyle header, CellStyle section,
                          CellStyle body, CellStyle center, CellStyle money) {
    }
}

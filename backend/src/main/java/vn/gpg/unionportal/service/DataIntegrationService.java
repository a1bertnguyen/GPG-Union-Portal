package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.FinanceEntryType;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.IntegrationType;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.model.IntegrationRun;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.repository.IntegrationRunRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.dto.ApiModels.IntegrationImportResult;
import vn.gpg.unionportal.spec.IntegrationRunSpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DataIntegrationService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Sort RUN_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final List<String> FINANCE_HEADERS = List.of(
            "entryCode", "unitCode", "transactionDate", "entryType", "category", "amount",
            "description", "documentNumber", "documentStatus");

    private final MemberCsvService memberCsvService;
    private final FinanceEntryRepository financeRepository;
    private final UnionUnitRepository unitRepository;
    private final IntegrationRunRepository runRepository;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public DataIntegrationService(MemberCsvService memberCsvService,
                                  FinanceEntryRepository financeRepository,
                                  UnionUnitRepository unitRepository,
                                  IntegrationRunRepository runRepository,
                                  RealtimeEventPublisher events,
                                  SpecAggregates aggregates) {
        this.memberCsvService = memberCsvService;
        this.financeRepository = financeRepository;
        this.unitRepository = unitRepository;
        this.runRepository = runRepository;
        this.events = events;
        this.aggregates = aggregates;
    }

    public IntegrationImportResult importHr(MultipartFile file, String username) {
        var result = memberCsvService.importMembers(file);
        var run = saveRun(IntegrationType.HR_IMPORT, fileName(file), result.totalRows(), result.importedRows(),
                result.errors(), username);
        return new IntegrationImportResult(run, result.createdRows(), result.updatedRows(), result.errors());
    }

    @Transactional
    public IntegrationImportResult importFinance(MultipartFile file, String username) {
        int total = 0;
        int created = 0;
        int updated = 0;
        var errors = new ArrayList<String>();

        if (file == null || file.isEmpty()) {
            errors.add("Vui lòng chọn tệp CSV tài chính có dữ liệu.");
        } else if (file.getSize() > MAX_FILE_SIZE) {
            errors.add("Tệp CSV không được lớn hơn 5 MB.");
        } else {
            try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    errors.add("Tệp CSV không có dòng tiêu đề.");
                } else {
                    var header = headerIndexes(stripBom(firstLine));
                    var missing = FINANCE_HEADERS.stream()
                            .filter(name -> !header.containsKey(name.toLowerCase(Locale.ROOT))).toList();
                    if (!missing.isEmpty()) {
                        errors.add("Thiếu cột bắt buộc: " + String.join(", ", missing));
                    } else {
                        String line;
                        int lineNumber = 1;
                        while ((line = reader.readLine()) != null) {
                            lineNumber++;
                            if (line.isBlank()) continue;
                            total++;
                            try {
                                var values = parseRow(line);
                                String entryCode = required(values, header, "entryCode");
                                var existing = financeRepository.findByEntryCodeIgnoreCase(entryCode);
                                var entry = existing.orElseGet(FinanceEntry::new);
                                entry.setEntryCode(entryCode);
                                String unitCode = required(values, header, "unitCode");
                                entry.setUnionUnit(unitRepository.findByCodeIgnoreCase(unitCode)
                                        .orElseThrow(() -> new IllegalArgumentException("không tìm thấy CĐCS có mã " + unitCode)));
                                entry.setTransactionDate(parseDate(required(values, header, "transactionDate")));
                                entry.setEntryType(parseEnum(FinanceEntryType.class,
                                        required(values, header, "entryType"), "entryType"));
                                entry.setCategory(required(values, header, "category"));
                                entry.setAmount(parseAmount(required(values, header, "amount")));
                                entry.setDescription(required(values, header, "description"));
                                entry.setDocumentNumber(optional(values, header, "documentNumber"));
                                entry.setDocumentStatus(parseEnum(DocumentStatus.class,
                                        required(values, header, "documentStatus"), "documentStatus"));
                                financeRepository.save(entry);
                                if (existing.isPresent()) updated++;
                                else created++;
                            } catch (IllegalArgumentException exception) {
                                errors.add("Dòng " + lineNumber + ": " + exception.getMessage());
                            }
                        }
                    }
                }
            } catch (IOException exception) {
                errors.add("Không thể đọc tệp CSV: " + exception.getMessage());
            }
        }

        int successful = created + updated;
        var run = saveRun(IntegrationType.FINANCE_IMPORT, fileName(file), total, successful, errors, username);
        if (successful > 0) {
            events.changed("finance", "BULK_IMPORTED", null, null);
        }
        return new IntegrationImportResult(run, created, updated, List.copyOf(errors));
    }

    @Transactional(readOnly = true)
    public byte[] exportFinance(YearMonth month, Long unitId) {
        var content = new StringBuilder("\uFEFF").append(String.join(",", FINANCE_HEADERS)).append("\r\n");
        financeRepository.findAll().stream()
                .filter(entry -> month == null || YearMonth.from(entry.getTransactionDate()).equals(month))
                .filter(entry -> unitId == null || entry.getUnionUnit().getId().equals(unitId))
                .sorted(Comparator.comparing(FinanceEntry::getTransactionDate).thenComparing(FinanceEntry::getEntryCode))
                .forEach(entry -> content.append(toCsvRow(List.of(
                        entry.getEntryCode(), entry.getUnionUnit().getCode(), entry.getTransactionDate().toString(),
                        entry.getEntryType().name(), entry.getCategory(), entry.getAmount().toPlainString(),
                        entry.getDescription(), nullable(entry.getDocumentNumber()), entry.getDocumentStatus().name()
                ))).append("\r\n"));
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<IntegrationRun> listRuns() {
        return runRepository.findAll(Specs.<IntegrationRun>matchAll(), RUN_SORT);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationRun> pageRuns(ListQuery query) {
        return runRepository.findAll(Specs.nullSafe(IntegrationRunSpecs.filter(query)), query.pageable(RUN_SORT));
    }

    @Transactional(readOnly = true)
    public List<IntegrationRun> searchRuns(ListQuery query) {
        return runRepository.findAll(Specs.nullSafe(IntegrationRunSpecs.filter(query)), RUN_SORT);
    }

    @Transactional(readOnly = true)
    public ListFacets runFacets(ListQuery query) {
        Specification<IntegrationRun> filtered = Specs.nullSafe(IntegrationRunSpecs.filter(query));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", runRepository.count(filtered));
        metrics.put("completed", runRepository.count(filtered.and(Specs.eq("status", IntegrationStatus.COMPLETED))));
        metrics.put("partial", runRepository.count(filtered.and(Specs.eq("status", IntegrationStatus.PARTIAL))));
        metrics.put("failed", runRepository.count(filtered.and(Specs.eq("status", IntegrationStatus.FAILED))));
        return new ListFacets(
                runRepository.count(),
                aggregates.distinctValues(IntegrationRun.class, Specs.matchAll(), "status"),
                metrics);
    }

    private IntegrationRun saveRun(IntegrationType type, String fileName, int total, int successful,
                                   List<String> errors, String username) {
        var run = new IntegrationRun();
        run.setIntegrationType(type);
        run.setStatus(errors.isEmpty() ? IntegrationStatus.COMPLETED
                : successful > 0 ? IntegrationStatus.PARTIAL : IntegrationStatus.FAILED);
        run.setFileName(fileName);
        run.setTotalRows(total);
        run.setSuccessfulRows(successful);
        run.setFailedRows(Math.max(total - successful, 0));
        run.setStartedBy(username);
        run.setCompletedAt(Instant.now());
        run.setErrorSummary(errors.isEmpty() ? null : abbreviate(String.join(" | ", errors), 4000));
        return runRepository.save(run);
    }

    private String fileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) return "unknown.csv";
        return abbreviate(file.getOriginalFilename().replace('\\', '_').replace('/', '_'), 255);
    }

    private Map<String, Integer> headerIndexes(String line) {
        var values = parseRow(line);
        var indexes = new HashMap<String, Integer>();
        for (int i = 0; i < values.size(); i++) indexes.put(values.get(i).trim().toLowerCase(Locale.ROOT), i);
        return indexes;
    }

    private String required(List<String> values, Map<String, Integer> header, String name) {
        String value = optional(values, header, name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("cột " + name + " không được để trống");
        return value;
    }

    private String optional(List<String> values, Map<String, Integer> header, String name) {
        Integer index = header.get(name.toLowerCase(Locale.ROOT));
        if (index == null || index >= values.size()) return null;
        String value = values.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("ngày phải theo định dạng yyyy-MM-dd");
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            var amount = new BigDecimal(value);
            if (amount.signum() <= 0) throw new IllegalArgumentException("số tiền phải lớn hơn 0");
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("số tiền không hợp lệ: " + value);
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String fieldName) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("giá trị " + fieldName + " không hợp lệ: " + value);
        }
    }

    private List<String> parseRow(String line) {
        var values = new ArrayList<String>();
        var current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else current.append(character);
        }
        if (quoted) throw new IllegalArgumentException("dấu ngoặc kép trong CSV chưa được đóng");
        values.add(current.toString());
        return values;
    }

    private String toCsvRow(List<String> values) {
        return values.stream().map(this::escape).reduce((left, right) -> left + "," + right).orElse("");
    }

    private String escape(String raw) {
        String value = raw == null ? "" : raw;
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String nullable(Object value) {
        return value == null ? "" : value.toString();
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

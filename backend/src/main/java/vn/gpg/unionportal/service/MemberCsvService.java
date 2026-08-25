package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.dto.ApiModels.MemberImportResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MemberCsvService {
    private static final List<String> HEADERS = List.of(
            "employeeCode", "fullName", "unitCode", "jobTitle", "workplace", "joinDate",
            "membershipStatus", "employmentStatus", "email", "phone");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final MemberRepository memberRepository;
    private final UnionUnitRepository unitRepository;

    public MemberCsvService(MemberRepository memberRepository, UnionUnitRepository unitRepository) {
        this.memberRepository = memberRepository;
        this.unitRepository = unitRepository;
    }

    public byte[] exportMembers(Long unitId, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        var content = new StringBuilder("\uFEFF").append(String.join(",", HEADERS)).append("\r\n");

        memberRepository.findAll().stream()
                .filter(member -> unitId == null || member.getUnionUnit().getId().equals(unitId))
                .filter(member -> normalizedQuery.isEmpty()
                        || member.getFullName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || member.getEmployeeCode().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Member::getFullName))
                .forEach(member -> content.append(toCsvRow(List.of(
                        member.getEmployeeCode(), member.getFullName(), member.getUnionUnit().getCode(),
                        nullable(member.getJobTitle()), nullable(member.getWorkplace()), nullable(member.getJoinDate()),
                        member.getMembershipStatus().name(), member.getEmploymentStatus().name(),
                        nullable(member.getEmail()), nullable(member.getPhone())))).append("\r\n"));

        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public MemberImportResult importMembers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new MemberImportResult(0, 0, 0, 0, List.of("Vui lòng chọn tệp CSV có dữ liệu."));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return new MemberImportResult(0, 0, 0, 0, List.of("Tệp CSV không được lớn hơn 5 MB."));
        }

        int total = 0;
        int created = 0;
        int updated = 0;
        var errors = new ArrayList<String>();

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return new MemberImportResult(0, 0, 0, 0, List.of("Tệp CSV không có dòng tiêu đề."));
            }

            var header = headerIndexes(stripBom(firstLine));
            var missing = HEADERS.stream().filter(name -> !header.containsKey(name.toLowerCase(Locale.ROOT))).toList();
            if (!missing.isEmpty()) {
                return new MemberImportResult(0, 0, 0, 0,
                        List.of("Thiếu cột bắt buộc: " + String.join(", ", missing)));
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                total++;
                try {
                    var values = parseRow(line);
                    String employeeCode = required(values, header, "employeeCode");
                    String fullName = required(values, header, "fullName");
                    String unitCode = required(values, header, "unitCode");
                    var unit = unitRepository.findByCodeIgnoreCase(unitCode)
                            .orElseThrow(() -> new IllegalArgumentException("không tìm thấy CĐCS có mã " + unitCode));

                    var existing = memberRepository.findByEmployeeCodeIgnoreCase(employeeCode);
                    var member = existing.orElseGet(Member::new);
                    member.setEmployeeCode(employeeCode);
                    member.setFullName(fullName);
                    member.setUnionUnit(unit);
                    member.setJobTitle(optional(values, header, "jobTitle"));
                    member.setWorkplace(optional(values, header, "workplace"));
                    member.setJoinDate(parseDate(optional(values, header, "joinDate")));
                    member.setMembershipStatus(parseEnum(MembershipStatus.class,
                            required(values, header, "membershipStatus"), "membershipStatus"));
                    member.setEmploymentStatus(parseEnum(EmploymentStatus.class,
                            required(values, header, "employmentStatus"), "employmentStatus"));
                    member.setEmail(optional(values, header, "email"));
                    member.setPhone(optional(values, header, "phone"));
                    memberRepository.save(member);

                    if (existing.isPresent()) updated++;
                    else created++;
                } catch (IllegalArgumentException exception) {
                    errors.add("Dòng " + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            errors.add("Không thể đọc tệp CSV: " + exception.getMessage());
        }

        return new MemberImportResult(total, created + updated, created, updated, List.copyOf(errors));
    }

    private Map<String, Integer> headerIndexes(String line) {
        var values = parseRow(line);
        var indexes = new HashMap<String, Integer>();
        for (int i = 0; i < values.size(); i++) {
            indexes.put(values.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        return indexes;
    }

    private String required(List<String> values, Map<String, Integer> header, String name) {
        String value = optional(values, header, name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("cột " + name + " không được để trống");
        return value;
    }

    private String optional(List<String> values, Map<String, Integer> header, String name) {
        int index = header.get(name.toLowerCase(Locale.ROOT));
        if (index >= values.size()) return null;
        String value = values.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("ngày phải theo định dạng yyyy-MM-dd");
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
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
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
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
}

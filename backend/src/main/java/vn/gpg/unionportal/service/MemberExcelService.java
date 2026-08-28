package vn.gpg.unionportal.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.service.SpreadsheetImportService.ExportColumn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class MemberExcelService {
    private static final List<ExportColumn> COLUMNS = SpreadsheetImportService.memberExportColumns();
    private final MemberService members;

    public MemberExcelService(MemberService members) {
        this.members = members;
    }

    /**
     * Exports every member matching {@code query} — the same filters the table applies, resolved by
     * {@code MemberService.search}. The export deliberately ignores paging so users get the whole
     * filtered set rather than the page they happen to be looking at. Headers and column order come
     * from {@link SpreadsheetImportService#memberExportColumns()}, the same schema the import template
     * and validation use, so export/import/template never drift apart.
     */
    public byte[] exportMembers(ListQuery query) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dữ liệu");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            textStyle.setVerticalAlignment(VerticalAlignment.TOP);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            dateStyle.setVerticalAlignment(VerticalAlignment.TOP);
            Row header = sheet.createRow(0);
            header.setHeightInPoints(30);
            for (int index = 0; index < COLUMNS.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(COLUMNS.get(index).header());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, Math.min(Math.max(COLUMNS.get(index).header().length() + 4, 16), 30) * 256);
            }
            List<Member> data = members.search(query.withoutPaging());
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Member member = data.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < COLUMNS.size(); columnIndex++) {
                    ExportColumn column = COLUMNS.get(columnIndex);
                    if ("date".equals(column.kind())) {
                        date(row, columnIndex, (LocalDate) fieldValue(member, column.name()), dateStyle);
                    } else {
                        text(row, columnIndex, textValue(fieldValue(member, column.name())), textStyle);
                    }
                }
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(data.size(), 1), 0, COLUMNS.size() - 1));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xuất danh sách đoàn viên", exception);
        }
    }

    private Object fieldValue(Member member, String columnName) {
        return switch (columnName) {
            case "employeeCode" -> member.getEmployeeCode();
            case "fullName" -> member.getFullName();
            case "unitCode" -> member.getUnionUnit().getCode();
            case "company" -> member.getCompany();
            case "workplace" -> member.getWorkplace();
            case "proposedUnionTitle" -> member.getProposedUnionTitle();
            case "professionalTitle" -> member.getProfessionalTitle();
            case "jobTitle" -> member.getJobTitle();
            case "gender" -> member.getGender();
            case "ethnicity" -> member.getEthnicity();
            case "placeOfBirth" -> member.getPlaceOfBirth();
            case "nationalId" -> member.getNationalId();
            case "partyMember" -> member.isPartyMember();
            case "education" -> member.getEducation();
            case "specialization" -> member.getSpecialization();
            case "politicalTheory" -> member.getPoliticalTheory();
            case "foreignLanguage" -> member.getForeignLanguage();
            case "phone" -> member.getPhone();
            case "joinDate" -> member.getJoinDate();
            case "startWorkDate" -> member.getStartWorkDate();
            case "email" -> member.getEmail();
            case "currentResidence" -> member.getCurrentResidence();
            case "membershipStatus" -> member.getMembershipStatus().name();
            case "employmentStatus" -> member.getEmploymentStatus().name();
            default -> throw new IllegalStateException("Cột xuất Excel không xác định: " + columnName);
        };
    }

    private String textValue(Object value) {
        if (value == null) return "";
        if (value instanceof Boolean bool) return bool ? "Có" : "Không";
        if (value instanceof Enum<?> enumValue) return enumValue.name();
        return value.toString();
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private void text(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.STRING);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void date(Row row, int index, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }
}

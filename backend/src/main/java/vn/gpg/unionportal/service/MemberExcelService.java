package vn.gpg.unionportal.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.Member;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class MemberExcelService {
    private static final List<String> HEADERS = List.of(
            "Mã nhân viên", "Họ và tên", "Mã CĐCS", "Chức danh", "Nơi làm việc", "Ngày gia nhập công đoàn",
            "Tình trạng công đoàn", "Trạng thái nhân sự", "Email", "Số điện thoại");
    private static final int[] WIDTHS = {18, 28, 16, 22, 24, 22, 22, 20, 30, 18};
    private final MemberService members;

    public MemberExcelService(MemberService members) {
        this.members = members;
    }

    /**
     * Exports every member matching {@code query} — the same filters the table applies, resolved by
     * {@code MemberService.search}. The export deliberately ignores paging so users get the whole
     * filtered set rather than the page they happen to be looking at.
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
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, WIDTHS[index] * 256);
            }
            List<Member> data = members.search(query.withoutPaging());
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Member member = data.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                text(row, 0, member.getEmployeeCode(), textStyle);
                text(row, 1, member.getFullName(), textStyle);
                text(row, 2, member.getUnionUnit().getCode(), textStyle);
                text(row, 3, member.getJobTitle(), textStyle);
                text(row, 4, member.getWorkplace(), textStyle);
                date(row, 5, member.getJoinDate(), dateStyle);
                text(row, 6, member.getMembershipStatus().name(), textStyle);
                text(row, 7, member.getEmploymentStatus().name(), textStyle);
                text(row, 8, member.getEmail(), textStyle);
                text(row, 9, member.getPhone(), textStyle);
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(data.size(), 1), 0, HEADERS.size() - 1));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xuất danh sách đoàn viên", exception);
        }
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

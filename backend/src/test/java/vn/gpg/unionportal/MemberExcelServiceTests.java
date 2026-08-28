package vn.gpg.unionportal;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.MemberExcelService;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MemberExcelServiceTests {
    @Autowired
    private MemberExcelService service;

    private static ListQuery query(String status, String preset) {
        return new ListQuery(null, null, null, "", "all", null, status, preset);
    }

    @Test
    void exportsReadableVietnameseWorkbookAndAppliesUiFilters() throws Exception {
        byte[] bytes = service.exportMembers(query("NOT_JOINED", null));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Dữ liệu");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Mã nhân viên");
            assertThat(sheet.getRow(0).getCell(17).getStringCellValue()).isEqualTo("ĐT di động");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(22).getStringCellValue()).isEqualTo("NOT_JOINED");
            assertThat(sheet.getRow(1).getCell(17).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(17).getStringCellValue()).startsWith("0");
        }
    }

    @Test
    void exportsOnlyRowsWithMissingProfileDataWhenRequested() throws Exception {
        byte[] bytes = service.exportMembers(query(null, "missing"));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Dữ liệu");
            assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(1);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                // Mirrors MemberSpecs.REQUIRED_FIELDS: jobTitle(7), workplace(4), joinDate(18), email(20), phone(17).
                boolean missing = row.getCell(7).getStringCellValue().isBlank()
                        || row.getCell(4).getStringCellValue().isBlank()
                        || row.getCell(18) == null || row.getCell(18).getCellType() == CellType.BLANK
                        || row.getCell(20).getStringCellValue().isBlank()
                        || row.getCell(17).getStringCellValue().isBlank();
                assertThat(missing).as("row %s", rowIndex + 1).isTrue();
            }
        }
    }

    @Test
    void exportIgnoresPagingSoUsersGetTheWholeFilteredSet() throws Exception {
        byte[] onePage = service.exportMembers(new ListQuery(0, 1, null, "", "all", null, null, null));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(onePage))) {
            assertThat(workbook.getSheet("Dữ liệu").getLastRowNum()).isGreaterThan(1);
        }
    }
}

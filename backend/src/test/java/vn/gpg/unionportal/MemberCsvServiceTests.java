package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.service.MemberCsvService;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberCsvServiceTests {
    @Autowired
    private MemberCsvService csvService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void exportsUtf8CsvThatCanBeOpenedByExcel() {
        String csv = new String(csvService.exportMembers(null, ""), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFemployeeCode,fullName,unitCode");
        assertThat(csv).contains("\"NV3811\"");
    }

    @Test
    void importsNewMemberAndUpdatesByEmployeeCode() {
        var created = csv("""
                employeeCode,fullName,unitCode,jobTitle,workplace,joinDate,membershipStatus,employmentStatus,email,phone
                CSV-TEST-001,Nguyễn Thị Kiểm Thử,VCS,Chuyên viên,HCM,2026-08-20,MEMBER,ACTIVE,test@gpg.vn,0900000000
                """);

        var createResult = csvService.importMembers(created);
        assertThat(createResult.importedRows()).isEqualTo(1);
        assertThat(createResult.createdRows()).isEqualTo(1);
        assertThat(createResult.errors()).isEmpty();

        var updated = csv("""
                employeeCode,fullName,unitCode,jobTitle,workplace,joinDate,membershipStatus,employmentStatus,email,phone
                CSV-TEST-001,Nguyễn Thị Đã Cập Nhật,VCS,Trưởng nhóm,HCM,2026-08-20,MEMBER,ACTIVE,test@gpg.vn,0900000000
                """);
        var updateResult = csvService.importMembers(updated);

        assertThat(updateResult.updatedRows()).isEqualTo(1);
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("CSV-TEST-001"))
                .get().extracting(member -> member.getFullName()).isEqualTo("Nguyễn Thị Đã Cập Nhật");
    }

    @Test
    void reportsInvalidUnitWithoutImportingTheRow() {
        var result = csvService.importMembers(csv("""
                employeeCode,fullName,unitCode,jobTitle,workplace,joinDate,membershipStatus,employmentStatus,email,phone
                CSV-BAD-001,Dữ liệu lỗi,KHONG-TON-TAI,,,,MEMBER,ACTIVE,,
                """));

        assertThat(result.importedRows()).isZero();
        assertThat(result.errors()).singleElement().asString().contains("không tìm thấy CĐCS");
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("CSV-BAD-001")).isEmpty();
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "members.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}

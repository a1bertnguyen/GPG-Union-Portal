package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ActivityRequest;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.ActivityMediaService;
import vn.gpg.unionportal.service.ActivityService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ActivityProgramReportWorkflowTests {
    @Autowired private ActivityService activities;
    @Autowired private ActivityMediaService media;
    @Autowired private UnionUnitRepository units;

    @Test
    void requiresEvidenceForSubmissionAndFollowUpAssignmentForClosure() {
        UnionUnit unit = units.findAll().getFirst();
        var draft = activities.create(request(unit.getId(), ActivityStatus.IN_PROGRESS, false, null, null));

        assertThatThrownBy(() -> activities.update(draft.getId(),
                request(unit.getId(), ActivityStatus.IN_PROGRESS, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 1 ảnh")
                .hasMessageContaining("ít nhất 1 chứng từ");

        media.upload(draft.getId(), ActivityMediaType.PHOTO, "Ảnh chương trình",
                new MockMultipartFile("file", "chuong-trinh.png", "image/png", new byte[]{1, 2, 3}));
        media.upload(draft.getId(), ActivityMediaType.DOCUMENT, "Chứng từ",
                new MockMultipartFile("file", "chung-tu.pdf", "application/pdf", new byte[]{4, 5, 6}));

        var submitted = activities.update(draft.getId(),
                request(unit.getId(), ActivityStatus.IN_PROGRESS, true, null, null));
        assertThat(submitted.getReportCompleted()).isTrue();
        assertThat(submitted.getStatus()).isEqualTo(ActivityStatus.IN_PROGRESS);

        assertThatThrownBy(() -> activities.update(draft.getId(),
                request(unit.getId(), ActivityStatus.COMPLETED, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIC và deadline");

        var closed = activities.update(draft.getId(), request(unit.getId(), ActivityStatus.COMPLETED, true,
                "Nguyễn Văn PIC", LocalDate.of(2026, 9, 20)));
        assertThat(closed.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(closed.getFollowUpOwner()).isEqualTo("Nguyễn Văn PIC");
    }

    private ActivityRequest request(Long unitId, ActivityStatus status, boolean reportCompleted,
                                    String followUpOwner, LocalDate followUpDeadline) {
        return new ActivityRequest(
                "ACT-REPORT-TEST", "Chương trình kiểm thử báo cáo", unitId,
                LocalDate.of(2026, 9, 12), LocalTime.of(9, 0), "Hội trường A", "Trần Thu Hà",
                status, "Tăng kết nối người lao động", new BigDecimal("5000000"), new BigDecimal("4500000"),
                50, 42, "Danh sách 42 người tham dự", "NLĐ khối vận hành", 42,
                "Đã triển khai đầy đủ các nội dung kết nối", "Điều chỉnh thời lượng phần hỏi đáp", 42,
                new BigDecimal("4.60"), "Phản hồi tích cực", "Không phát sinh vấn đề nghiêm trọng",
                "Duy trì chương trình hằng quý", "Bài truyền thông nội bộ và album ảnh",
                "Check-in nhanh, nội dung thực tế", "Thời lượng hỏi đáp còn ngắn",
                reportCompleted, DocumentStatus.COMPLETE, "Theo dõi đề xuất tổ chức quý sau",
                followUpOwner, followUpDeadline, "IN_PROGRESS", "Chuẩn bị câu hỏi trước chương trình");
    }
}

package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.MemberChangeRequest;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.service.ActivityMediaService;
import vn.gpg.unionportal.service.MemberWorkspaceService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OperationalWorkspaceServiceTests {
    @Autowired private MemberWorkspaceService memberWorkspace;
    @Autowired private ActivityMediaService activityMedia;
    @Autowired private MemberRepository members;
    @Autowired private UnionActivityRepository activities;

    @Test
    void storesMemberHistoryDocumentsAndActivityPhotos() {
        var member = members.findAll().getFirst();
        var change = memberWorkspace.createChange(new MemberChangeRequest(
                member.getId(), "THAY ĐỔI CHỨC DANH", LocalDate.of(2026, 8, 25), "Bổ nhiệm vị trí mới"));
        assertThat(memberWorkspace.listChanges(member.getId())).extracting("id").contains(change.id());

        var pdf = new MockMultipartFile("file", "don-gia-nhap.pdf", "application/pdf", "pdf-content".getBytes());
        var document = memberWorkspace.uploadDocument(member.getId(), MemberDocumentType.JOIN_APPLICATION, pdf);
        assertThat(memberWorkspace.listDocuments(member.getId())).extracting("id").contains(document.id());
        assertThat(memberWorkspace.downloadDocument(document.id()).data()).isEqualTo("pdf-content".getBytes());

        var activity = activities.findAll().getFirst();
        var image = new MockMultipartFile("file", "check-in.png", "image/png", new byte[]{1, 2, 3, 4});
        var media = activityMedia.upload(activity.getId(), ActivityMediaType.PHOTO, "Ảnh check-in", image);
        assertThat(activityMedia.list(activity.getId())).extracting("id").contains(media.id());
        assertThat(activityMedia.download(media.id()).data()).containsExactly(1, 2, 3, 4);
    }
}

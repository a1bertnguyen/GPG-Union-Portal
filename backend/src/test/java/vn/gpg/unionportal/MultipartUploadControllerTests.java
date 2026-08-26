package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.controller.ActivityMediaController;
import vn.gpg.unionportal.controller.MemberWorkspaceController;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;
import vn.gpg.unionportal.service.ActivityMediaService;
import vn.gpg.unionportal.service.MemberWorkspaceService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MultipartUploadControllerTests {

    @Test
    void bindsBrowserFormDataForActivityImageUpload() throws Exception {
        ActivityMediaService service = mock(ActivityMediaService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ActivityMediaController(service)).build();
        var file = new MockMultipartFile("file", "anh.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/activity-media")
                        .file(file)
                        .param("activityId", "42")
                        .param("mediaType", "PHOTO")
                        .param("title", "Ảnh chương trình"))
                .andExpect(status().isCreated());

        verify(service).upload(eq(42L), eq(ActivityMediaType.PHOTO), eq("Ảnh chương trình"), any(MultipartFile.class));
    }

    @Test
    void bindsBrowserFormDataForMemberDocumentUpload() throws Exception {
        MemberWorkspaceService service = mock(MemberWorkspaceService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MemberWorkspaceController(service)).build();
        var file = new MockMultipartFile("file", "don-gia-nhap.pdf", "application/pdf", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/member-documents")
                        .file(file)
                        .param("memberId", "17")
                        .param("documentType", "JOIN_APPLICATION"))
                .andExpect(status().isCreated());

        verify(service).uploadDocument(eq(17L), eq(MemberDocumentType.JOIN_APPLICATION), any(MultipartFile.class));
    }
}

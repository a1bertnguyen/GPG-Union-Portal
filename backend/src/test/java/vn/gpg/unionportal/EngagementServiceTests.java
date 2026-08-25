package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.service.EngagementService;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyResponseRequest;
import vn.gpg.unionportal.service.PulseSurveyService;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class EngagementServiceTests {
    @Autowired
    private EngagementService engagementService;

    @Autowired
    private PulseSurveyService surveyService;

    @Test
    void summarizesEmployeeVoiceKpisFromOperationalData() {
        var summary = engagementService.summary(YearMonth.of(2026, 8), null);

        assertThat(summary.totalSurveyCount()).isEqualTo(2);
        assertThat(summary.activeSurveyCount()).isEqualTo(1);
        assertThat(summary.totalResponses()).isEqualTo(4);
        assertThat(summary.surveyResponseRate()).isEqualTo(66.7);
        assertThat(summary.averageRating()).isEqualTo(4.0);
        assertThat(summary.caseResponseRate()).isEqualTo(66.7);
        assertThat(summary.averageActivityScore()).isEqualTo(4.6);
        assertThat(summary.topNeeds()).first()
                .satisfies(need -> {
                    assertThat(need.category()).isEqualTo("Sức khỏe tinh thần");
                    assertThat(need.count()).isEqualTo(2);
                });
        assertThat(summary.alerts()).extracting(alert -> alert.title())
                .contains("Kiến nghị chưa được phản hồi đầy đủ");
    }

    @Test
    void filtersEngagementSummaryByUnionUnit() {
        var summary = engagementService.summary(YearMonth.of(2026, 8), 1L);

        assertThat(summary.totalSurveyCount()).isEqualTo(1);
        assertThat(summary.totalResponses()).isEqualTo(3);
        assertThat(summary.surveyResponseRate()).isEqualTo(75.0);
        assertThat(summary.caseResponseRate()).isEqualTo(100.0);
    }

    @Test
    void acceptsAnonymousResponseForActiveSurvey() {
        var response = surveyService.respond(1L,
                new PulseSurveyResponseRequest(5, "Kết nối nội bộ", "Thêm hoạt động giao lưu", true, null));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getRespondentName()).isNull();
        assertThat(response.getSurvey().getId()).isEqualTo(1L);
    }

    @Test
    void rejectsResponseWhenSurveyIsClosed() {
        assertThatThrownBy(() -> surveyService.respond(2L,
                new PulseSurveyResponseRequest(4, "Phúc lợi", null, true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không mở nhận phản hồi");
    }
}

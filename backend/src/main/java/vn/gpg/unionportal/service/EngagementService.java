package vn.gpg.unionportal.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.model.DomainEnums.SurveyStatus;
import vn.gpg.unionportal.model.PulseSurvey;
import vn.gpg.unionportal.model.PulseSurveyResponse;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.repository.PulseSurveyRepository;
import vn.gpg.unionportal.repository.PulseSurveyResponseRepository;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.dto.ApiModels.AlertItem;
import vn.gpg.unionportal.dto.ApiModels.EngagementSummary;
import vn.gpg.unionportal.dto.ApiModels.NeedCount;
import vn.gpg.unionportal.spec.Specs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EngagementService {
    private final PulseSurveyRepository surveyRepository;
    private final PulseSurveyResponseRepository responseRepository;
    private final LaborCaseRepository laborCaseRepository;
    private final UnionActivityRepository activityRepository;

    public EngagementService(PulseSurveyRepository surveyRepository,
                             PulseSurveyResponseRepository responseRepository,
                             LaborCaseRepository laborCaseRepository,
                             UnionActivityRepository activityRepository) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.laborCaseRepository = laborCaseRepository;
        this.activityRepository = activityRepository;
    }

    public EngagementSummary summary(YearMonth month, Long unitId) {
        var start = month.atDay(1);
        var end = month.atEndOfMonth();
        Specification<PulseSurvey> surveyFilter = Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId),
                (root, query, cb) -> cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), end),
                        cb.greaterThanOrEqualTo(root.get("endDate"), start))));
        var surveys = surveyRepository.findAll(surveyFilter);
        var surveyIds = surveys.stream().map(PulseSurvey::getId).collect(Collectors.toSet());
        Specification<PulseSurveyResponse> selectedSurveys = surveyIds.isEmpty()
                ? Specs.none()
                : (root, query, cb) -> root.get("survey").get("id").in(surveyIds);
        Specification<PulseSurveyResponse> responseFilter = Specs.nullSafe(Specs.allOf(
                Specs.unitScopeVia("survey", unitId),
                Specs.inMonth("submittedOn", month),
                selectedSurveys));
        var responses = responseRepository.findAll(responseFilter);

        long targetResponses = surveys.stream().mapToLong(PulseSurvey::getTargetResponses).sum();
        double surveyResponseRate = percentage(responses.size(), targetResponses);
        double averageRating = round(responses.stream().mapToInt(PulseSurveyResponse::getRating).average().orElse(0));

        Specification<LaborCase> caseFilter = Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("receivedDate", month)));
        var cases = laborCaseRepository.findAll(caseFilter);
        long answeredCases = cases.stream().filter(item -> item.getResultText() != null && !item.getResultText().isBlank()).count();
        double caseResponseRate = cases.isEmpty() ? 100 : percentage(answeredCases, cases.size());

        Specification<UnionActivity> activityFilter = Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("eventDate", month)));
        var activityScores = activityRepository.findAll(activityFilter).stream()
                .map(item -> item.getUsefulnessScore())
                .filter(score -> score != null)
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
        double averageActivityScore = round(java.util.Arrays.stream(activityScores).average().orElse(0));

        Map<String, Long> needCounts = responses.stream().map(PulseSurveyResponse::getNeedCategory)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<NeedCount> topNeeds = needCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new NeedCount(entry.getKey(), entry.getValue()))
                .toList();

        var alerts = new java.util.ArrayList<AlertItem>();
        if (!surveys.isEmpty() && surveyResponseRate < 60) {
            alerts.add(new AlertItem("warning", "Tỷ lệ phản hồi khảo sát thấp",
                    "Mới đạt " + surveyResponseRate + "% so với chỉ tiêu tối thiểu 60%."));
        }
        if (!responses.isEmpty() && averageRating < 3.5) {
            alerts.add(new AlertItem("danger", "Điểm kết nối cần chú ý",
                    "Điểm trung bình " + averageRating + "/5, thấp hơn ngưỡng 3.5."));
        }
        if (!cases.isEmpty() && caseResponseRate < 90) {
            alerts.add(new AlertItem("warning", "Kiến nghị chưa được phản hồi đầy đủ",
                    "Tỷ lệ có kết quả/phản hồi đang ở mức " + caseResponseRate + "% (mục tiêu 90%)."));
        }
        if (activityScores.length > 0 && averageActivityScore < 3.5) {
            alerts.add(new AlertItem("warning", "Hoạt động có điểm hữu ích thấp",
                    "Điểm hữu ích trung bình " + averageActivityScore + "/5."));
        }

        return new EngagementSummary(
                month.toString(),
                surveys.stream().filter(survey -> survey.getStatus() == SurveyStatus.ACTIVE).count(),
                surveys.size(),
                responses.size(),
                surveyResponseRate,
                averageRating,
                caseResponseRate,
                averageActivityScore,
                topNeeds,
                List.copyOf(alerts));
    }

    private double percentage(long value, long total) {
        return total == 0 ? 0 : round(value * 100d / total);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

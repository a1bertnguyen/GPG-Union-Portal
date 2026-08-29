package vn.gpg.unionportal.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.KpiCriterionView;
import vn.gpg.unionportal.dto.ApiModels.UnitKpiView;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.spec.Specs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class KpiService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Bangkok");

    private final UnionUnitRepository units;
    private final WelfareRecordRepository welfare;
    private final LaborCaseRepository cases;
    private final PulseSurveyRepository surveys;
    private final PulseSurveyResponseRepository responses;
    private final UnionActivityRepository activities;
    private final MemberRepository members;
    private final CurrentUserService currentUser;

    public KpiService(UnionUnitRepository units, WelfareRecordRepository welfare,
                      LaborCaseRepository cases, PulseSurveyRepository surveys,
                      PulseSurveyResponseRepository responses, UnionActivityRepository activities,
                      MemberRepository members, CurrentUserService currentUser) {
        this.units = units;
        this.welfare = welfare;
        this.cases = cases;
        this.surveys = surveys;
        this.responses = responses;
        this.activities = activities;
        this.members = members;
        this.currentUser = currentUser;
    }

    public List<UnitKpiView> evaluate(YearMonth month, Long requestedUnitId) {
        Long scopedUnitId = currentUser.scopedUnitId(requestedUnitId);
        List<UnionUnit> selected = scopedUnitId == null
                ? units.findAll(Sort.by("code"))
                : List.of(units.findById(scopedUnitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + scopedUnitId)));
        return selected.stream().map(unit -> evaluateUnit(unit, month)).toList();
    }

    private UnitKpiView evaluateUnit(UnionUnit unit, YearMonth month) {
        Long unitId = unit.getId();
        var welfareRows = welfare.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("eventDate", month))));
        var caseRows = cases.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("receivedDate", month))));
        var surveyRows = surveys.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), (root, query, cb) -> cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), month.atEndOfMonth()),
                        cb.greaterThanOrEqualTo(root.get("endDate"), month.atDay(1))))));
        var activityRows = activities.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("eventDate", month))));
        var previousActivities = activities.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.inMonth("eventDate", month.minusMonths(1)))));
        var memberRows = members.findAll(Specs.nullSafe(Specs.allOf(
                Specs.unitScope(unitId), Specs.eq("employmentStatus", EmploymentStatus.ACTIVE))));

        List<KpiCriterionView> criteria = new ArrayList<>();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        var dueWelfare = welfareRows.stream()
                .filter(item -> item.getStatus() == WorkStatus.COMPLETED
                        || (item.getDeadline() != null && !item.getDeadline().isAfter(today)))
                .toList();
        long onTimeWelfare = dueWelfare.stream().filter(item -> item.getStatus() == WorkStatus.COMPLETED
                && item.getDeadline() != null
                && !LocalDate.ofInstant(item.getUpdatedAt(), BUSINESS_ZONE).isAfter(item.getDeadline())).count();
        double welfareRate = percentage(onTimeWelfare, dueWelfare.size(), 100);
        criteria.add(percent("WELFARE_ON_TIME", "Chính sách chăm lo đúng hạn", "≥95%",
                welfareRate, 95, onTimeWelfare + "/" + dueWelfare.size() + " phiếu đến hạn"));

        long assignedCases = caseRows.stream().filter(item -> present(item.getOwnerName()) && item.getDeadline() != null).count();
        double assignmentRate = percentage(assignedCases, caseRows.size(), 100);
        criteria.add(percent("CASE_ASSIGNMENT", "Vụ việc có PIC & deadline", "100%",
                assignmentRate, 100, assignedCases + "/" + caseRows.size() + " vụ việc"));

        long answeredCases = caseRows.stream().filter(item -> present(item.getResultText()) && item.getResponseDate() != null).count();
        double responseRate = percentage(answeredCases, caseRows.size(), 100);
        criteria.add(percent("CASE_RESPONSE", "Kiến nghị có phản hồi/kết quả", "≥90%",
                responseRate, 90, answeredCases + "/" + caseRows.size() + " vụ việc"));

        long targetResponses = surveyRows.stream().mapToLong(PulseSurvey::getTargetResponses).sum();
        var surveyIds = surveyRows.stream().map(PulseSurvey::getId).toList();
        long actualResponses = surveyIds.isEmpty() ? 0 : responses.count(Specs.nullSafe(Specs.allOf(
                Specs.unitScopeVia("survey", unitId), Specs.inMonth("submittedOn", month),
                (root, query, cb) -> root.get("survey").get("id").in(surveyIds))));
        double surveyRate = percentage(actualResponses, targetResponses, 0);
        criteria.add(percent("SURVEY_RESPONSE", "CBNV phản hồi khảo sát", "≥60%",
                surveyRate, 60, actualResponses + "/" + targetResponses + " phản hồi"));

        var scoredActivities = activityRows.stream().map(UnionActivity::getUsefulnessScore)
                .filter(java.util.Objects::nonNull).toList();
        double usefulRate = scoredActivities.isEmpty() ? 0 : round(scoredActivities.stream()
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0) * 20);
        criteria.add(percent("ACTIVITY_USEFUL", "Người tham dự đánh giá hữu ích", "≥70%",
                usefulRate, 70, scoredActivities.size() + " chương trình có đánh giá"));

        long reportedActivities = activityRows.stream().filter(item -> Boolean.TRUE.equals(item.getReportCompleted())).count();
        double reportRate = percentage(reportedActivities, activityRows.size(), 100);
        criteria.add(percent("ACTIVITY_REPORT", "Chương trình có báo cáo sau hoạt động", "100%",
                reportRate, 100, reportedActivities + "/" + activityRows.size() + " chương trình"));

        long bchMeetings = activityRows.stream().filter(this::isBchMeeting).count();
        criteria.add(new KpiCriterionView("BCH_MEETING", "BCH họp định kỳ", "≥1/tháng", bchMeetings,
                bchMeetings + " cuộc", bchMeetings >= 1,
                "Nhận diện từ tên/mục tiêu chương trình có BCH hoặc Ban chấp hành"));

        long completeMembers = memberRows.stream().filter(this::hasCompleteCoreProfile).count();
        double memberRate = percentage(completeMembers, memberRows.size(), 100);
        criteria.add(percent("MEMBER_DATA", "Dữ liệu đoàn viên cập nhật", "100%",
                memberRate, 100, completeMembers + "/" + memberRows.size() + " hồ sơ cốt lõi đầy đủ"));

        long unexplainedOverdue = caseRows.stream().filter(item -> item.getStatus() != CaseStatus.CLOSED)
                .filter(item -> item.getDeadline() != null && item.getDeadline().isBefore(today))
                .filter(item -> !present(item.getOverdueReason())).count();
        criteria.add(new KpiCriterionView("UNEXPLAINED_OVERDUE", "Vấn đề quá hạn không giải trình", "0",
                unexplainedOverdue, String.valueOf(unexplainedOverdue), unexplainedOverdue == 0,
                "Vụ chưa đóng, đã quá deadline nhưng chưa có lý do/ETA mới"));

        long currentParticipants = activityRows.stream().mapToLong(item -> safeInt(item.getParticipantCount())).sum();
        long previousParticipants = previousActivities.stream().mapToLong(item -> safeInt(item.getParticipantCount())).sum();
        long currentInvited = activityRows.stream().mapToLong(item -> safeInt(item.getInvitedCount())).sum();
        long previousInvited = previousActivities.stream().mapToLong(item -> safeInt(item.getInvitedCount())).sum();
        double currentParticipationRate = percentage(currentParticipants, currentInvited, 0);
        double previousParticipationRate = percentage(previousParticipants, previousInvited, 0);
        boolean participationMet = currentParticipationRate >= previousParticipationRate;
        String trend = currentParticipationRate > previousParticipationRate ? "Tăng"
                : currentParticipationRate < previousParticipationRate ? "Giảm" : "Ổn định";
        criteria.add(new KpiCriterionView("PARTICIPATION_TREND", "Tỷ lệ tham gia tự nguyện", "Tăng dần",
                round(currentParticipationRate - previousParticipationRate), trend, participationMet,
                currentParticipationRate + "% tháng này · " + previousParticipationRate + "% tháng trước"));

        long passed = criteria.stream().filter(KpiCriterionView::met).count();
        int score = Math.toIntExact(passed * 10);
        return new UnitKpiView(unitId, unit.getCode(), unit.getName(), month.toString(), score,
                rating(score), passed, List.copyOf(criteria));
    }

    private KpiCriterionView percent(String code, String label, String target, double actual,
                                     double threshold, String note) {
        return new KpiCriterionView(code, label, target, actual, round(actual) + "%", actual >= threshold, note);
    }

    private double percentage(long value, long total, double emptyValue) {
        return total == 0 ? emptyValue : round(value * 100d / total);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean isBchMeeting(UnionActivity activity) {
        String text = ((activity.getName() == null ? "" : activity.getName()) + " "
                + (activity.getObjective() == null ? "" : activity.getObjective())).toLowerCase(Locale.ROOT);
        return text.contains("bch") || text.contains("ban chấp hành") || text.contains("ban chap hanh");
    }

    private boolean hasCompleteCoreProfile(Member member) {
        return present(member.getEmployeeCode()) && present(member.getFullName())
                && present(member.getWorkplace()) && member.getStartWorkDate() != null;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String rating(int score) {
        if (score >= 90) return "XUẤT SẮC";
        if (score >= 80) return "TỐT";
        if (score >= 65) return "ĐẠT";
        return "CẦN CẢI THIỆN";
    }
}

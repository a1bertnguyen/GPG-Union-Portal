package vn.gpg.unionportal.mapper;

import org.springframework.stereotype.Component;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.dto.ApiModels.*;

import java.time.Instant;
import java.time.YearMonth;

@Component
public class EntityMapper {
    private final UnionUnitRepository unitRepository;

    public EntityMapper(UnionUnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public UnionUnit requireUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + id));
    }

    public UnionUnit apply(UnionUnit entity, UnionUnitRequest request) {
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setCompanyName(request.companyName().trim());
        entity.setLocation(trimToNull(request.location()));
        entity.setChairperson(trimToNull(request.chairperson()));
        entity.setTermStart(request.termStart());
        entity.setTermEnd(request.termEnd());
        entity.setDecisionNumber(trimToNull(request.decisionNumber()));
        entity.setLegalStatus(request.legalStatus());
        entity.setContactPerson(trimToNull(request.contactPerson()));
        return entity;
    }

    public Member apply(Member entity, MemberRequest request) {
        entity.setEmployeeCode(request.employeeCode().trim());
        entity.setFullName(request.fullName().trim());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setJobTitle(trimToNull(request.jobTitle()));
        entity.setWorkplace(trimToNull(request.workplace()));
        entity.setJoinDate(request.joinDate());
        entity.setMembershipStatus(request.membershipStatus());
        entity.setEmploymentStatus(request.employmentStatus());
        entity.setEmail(trimToNull(request.email()));
        entity.setPhone(trimToNull(request.phone()));
        return entity;
    }

    public WelfareRecord apply(WelfareRecord entity, WelfareRequest request) {
        entity.setRecordCode(request.recordCode().trim());
        entity.setWelfareType(request.welfareType());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setBeneficiaryName(request.beneficiaryName().trim());
        entity.setEventDate(request.eventDate());
        entity.setStatus(request.status());
        entity.setAmount(request.amount());
        entity.setDocumentStatus(request.documentStatus());
        entity.setNotes(trimToNull(request.notes()));
        return entity;
    }

    public LaborCase apply(LaborCase entity, LaborCaseRequest request) {
        entity.setCaseCode(request.caseCode().trim());
        entity.setReceivedDate(request.receivedDate());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setIssueGroup(request.issueGroup().trim());
        entity.setSeverity(request.severity());
        entity.setOwnerName(request.ownerName().trim());
        entity.setDeadline(request.deadline());
        entity.setStatus(request.status());
        entity.setDescription(request.description().trim());
        entity.setAffectedPeople(request.affectedPeople());
        entity.setResultText(trimToNull(request.resultText()));
        entity.setOverdueReason(trimToNull(request.overdueReason()));
        return entity;
    }

    public UnionActivity apply(UnionActivity entity, ActivityRequest request) {
        entity.setActivityCode(request.activityCode().trim());
        entity.setName(request.name().trim());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setEventDate(request.eventDate());
        entity.setStatus(request.status());
        entity.setObjective(trimToNull(request.objective()));
        entity.setPlannedBudget(request.plannedBudget());
        entity.setActualCost(request.actualCost());
        entity.setParticipantCount(request.participantCount());
        entity.setUsefulnessScore(request.usefulnessScore());
        entity.setReportCompleted(request.reportCompleted());
        entity.setFollowUpOwner(trimToNull(request.followUpOwner()));
        entity.setFollowUpDeadline(request.followUpDeadline());
        return entity;
    }

    public FinanceEntry apply(FinanceEntry entity, FinanceRequest request) {
        entity.setEntryCode(request.entryCode().trim());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setTransactionDate(request.transactionDate());
        entity.setEntryType(request.entryType());
        entity.setCategory(request.category().trim());
        entity.setAmount(request.amount());
        entity.setDescription(request.description().trim());
        entity.setDocumentNumber(trimToNull(request.documentNumber()));
        entity.setDocumentStatus(request.documentStatus());
        return entity;
    }

    public MonthlyReport apply(MonthlyReport entity, MonthlyReportRequest request) {
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setReportMonth(YearMonth.parse(request.month()).atDay(1));
        entity.setPreparedBy(request.preparedBy().trim());
        entity.setPlanNextMonth(trimToNull(request.planNextMonth()));
        entity.setSupportRequest(trimToNull(request.supportRequest()));
        entity.setStatus(request.status());
        if (request.status() == vn.gpg.unionportal.model.DomainEnums.ReportStatus.SUBMITTED
                || request.status() == vn.gpg.unionportal.model.DomainEnums.ReportStatus.APPROVED) {
            if (entity.getSubmittedAt() == null) {
                entity.setSubmittedAt(Instant.now());
            }
        } else {
            entity.setSubmittedAt(null);
        }
        return entity;
    }

    public PulseSurvey apply(PulseSurvey entity, PulseSurveyRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Ngày kết thúc khảo sát phải từ ngày bắt đầu trở đi");
        }
        entity.setSurveyCode(request.surveyCode().trim());
        entity.setTitle(request.title().trim());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setQuestionText(request.questionText().trim());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
        entity.setTargetResponses(request.targetResponses());
        return entity;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

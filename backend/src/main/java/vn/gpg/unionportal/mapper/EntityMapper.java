package vn.gpg.unionportal.mapper;

import org.springframework.stereotype.Component;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.dto.ApiModels.*;
import vn.gpg.unionportal.validation.MemberProfileCatalog;

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
        MemberProfileCatalog.validate(request.company(), request.workplace());
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
        entity.setCompany(trimToNull(request.company()));
        entity.setProposedUnionTitle(trimToNull(request.proposedUnionTitle()));
        entity.setProfessionalTitle(trimToNull(request.professionalTitle()));
        entity.setGender(request.gender());
        entity.setEthnicity(trimToNull(request.ethnicity()));
        entity.setPlaceOfBirth(trimToNull(request.placeOfBirth()));
        entity.setNationalId(trimToNull(request.nationalId()));
        entity.setPartyMember(Boolean.TRUE.equals(request.partyMember()));
        entity.setEducation(trimToNull(request.education()));
        entity.setSpecialization(trimToNull(request.specialization()));
        entity.setPoliticalTheory(trimToNull(request.politicalTheory()));
        entity.setForeignLanguage(trimToNull(request.foreignLanguage()));
        entity.setStartWorkDate(request.startWorkDate());
        entity.setCurrentResidence(trimToNull(request.currentResidence()));
        return entity;
    }

    public WelfareRecord apply(WelfareRecord entity, WelfareRequest request) {
        entity.setRecordCode(request.recordCode().trim());
        entity.setWelfareType(request.welfareType());
        entity.setPolicyName(trimToNull(request.policyName()));
        entity.setPolicyId(request.policyId());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setBeneficiaryName(request.beneficiaryName().trim());
        entity.setEventDate(request.eventDate());
        entity.setDeadline(request.deadline());
        entity.setStatus(request.status());
        entity.setAmount(request.amount());
        entity.setStandardAmount(request.standardAmount());
        entity.setDocumentStatus(request.documentStatus());
        entity.setReceiptStatus(request.receiptStatus());
        entity.setHasImage(request.hasImage());
        entity.setNotes(trimToNull(request.notes()));
        return entity;
    }

    public LaborCase apply(LaborCase entity, LaborCaseRequest request) {
        entity.setCaseCode(request.caseCode().trim());
        entity.setReceivedDate(request.receivedDate());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setRequesterName(request.requesterName().trim());
        entity.setEmployeeCode(trimToNull(request.employeeCode()));
        entity.setJobTitle(trimToNull(request.jobTitle()));
        entity.setWorkplace(trimToNull(request.workplace()));
        entity.setStartWorkDate(request.startWorkDate());
        entity.setLeaveDate(request.leaveDate());
        entity.setPhone(trimToNull(request.phone()));
        entity.setSource(trimToNull(request.source()));
        entity.setIssueGroup(request.issueGroup().trim());
        entity.setSeverity(request.severity());
        entity.setOwnerName(trimToNull(request.ownerName()));
        entity.setDeadline(request.deadline());
        entity.setStatus(request.status());
        entity.setDescription(request.description().trim());
        entity.setAffectedPeople(request.affectedPeople());
        entity.setAttachmentNote(trimToNull(request.attachmentNote()));
        entity.setResultText(trimToNull(request.resultText()));
        entity.setResponseDate(request.responseDate());
        entity.setOverdueReason(trimToNull(request.overdueReason()));
        return entity;
    }

    public UnionActivity apply(UnionActivity entity, ActivityRequest request) {
        entity.setActivityCode(request.activityCode().trim());
        entity.setName(request.name().trim());
        entity.setUnionUnit(requireUnit(request.unionUnitId()));
        entity.setEventDate(request.eventDate());
        entity.setEventTime(request.eventTime());
        entity.setLocation(trimToNull(request.location()));
        entity.setProgramPic(trimToNull(request.programPic()));
        entity.setStatus(request.status());
        entity.setObjective(trimToNull(request.objective()));
        entity.setPlannedBudget(request.plannedBudget());
        entity.setActualCost(request.actualCost());
        entity.setInvitedCount(request.invitedCount());
        entity.setParticipantCount(request.participantCount());
        entity.setParticipantList(trimToNull(request.participantList()));
        entity.setEmployeeGroup(trimToNull(request.employeeGroup()));
        entity.setCheckInCount(request.checkInCount());
        entity.setActualContent(trimToNull(request.actualContent()));
        entity.setPlanDifference(trimToNull(request.planDifference()));
        entity.setWorkersReached(request.workersReached());
        entity.setUsefulnessScore(request.usefulnessScore());
        entity.setQuickFeedback(trimToNull(request.quickFeedback()));
        entity.setIssues(trimToNull(request.issues()));
        entity.setOutputProposal(trimToNull(request.outputProposal()));
        entity.setCommunicationContent(trimToNull(request.communicationContent()));
        entity.setStrengths(trimToNull(request.strengths()));
        entity.setWeaknesses(trimToNull(request.weaknesses()));
        entity.setReportCompleted(request.reportCompleted());
        entity.setDocumentStatus(request.documentStatus());
        entity.setFollowUpIssue(trimToNull(request.followUpIssue()));
        entity.setFollowUpOwner(trimToNull(request.followUpOwner()));
        entity.setFollowUpDeadline(request.followUpDeadline());
        entity.setFollowUpStatus(trimToNull(request.followUpStatus()));
        entity.setLessonsLearned(trimToNull(request.lessonsLearned()));
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

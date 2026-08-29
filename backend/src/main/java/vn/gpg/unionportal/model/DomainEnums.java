package vn.gpg.unionportal.model;

public final class DomainEnums {
    private DomainEnums() {
    }

    public enum LegalStatus { ACTIVE, INACTIVE }
    public enum MembershipStatus { MEMBER, NOT_JOINED, LEFT }
    public enum Gender { MALE, FEMALE }
    public enum EmploymentStatus { ACTIVE, INACTIVE }
    public enum WelfareType { BIRTHDAY, FUNERAL, WEDDING, VISIT, CHILDBIRTH, HARDSHIP }
    public enum WelfarePolicySource { UNION, COMPANY }
    public enum WorkStatus { NEW, PENDING_APPROVAL, IN_PROGRESS, COMPLETED, CANCELLED }
    public enum DocumentStatus { COMPLETE, INCOMPLETE, NOT_REQUIRED }
    public enum CaseSeverity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum CaseStatus { NEW, VERIFYING, CLASSIFYING, ASSIGNED, IN_PROGRESS, WAITING_RESPONSE, PENDING_APPROVAL, CLOSED }
    public enum ActivityStatus { PLANNED, APPROVED, IN_PROGRESS, COMPLETED, CANCELLED }
    public enum FinanceEntryType { INCOME, EXPENSE, ADVANCE }
    public enum ReportStatus { DRAFT, SUBMITTED, APPROVED }
    public enum SurveyStatus { DRAFT, ACTIVE, CLOSED }
    public enum IntegrationType {
        HR_IMPORT, FINANCE_IMPORT,
        UNITS_IMPORT, MEMBERS_IMPORT, WELFARE_IMPORT, WELFARE_POLICIES_IMPORT, CASES_IMPORT, ACTIVITIES_IMPORT,
        FINANCE_EXCEL_IMPORT, SURVEYS_IMPORT, SURVEY_RESPONSES_IMPORT, REPORTS_IMPORT, USERS_IMPORT
    }
    public enum IntegrationStatus { COMPLETED, PARTIAL, FAILED }
    public enum MemberDocumentType { JOIN_APPLICATION, DECISION, BCH_DOCUMENT }
    public enum WelfareDocumentType { SUPPORTING_DOCUMENT, RECEIPT, IMAGE }
    public enum ActivityMediaType { PHOTO, DOCUMENT }
}

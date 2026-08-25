package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyResponseRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyView;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.SurveyStatus;
import vn.gpg.unionportal.model.PulseSurvey;
import vn.gpg.unionportal.model.PulseSurveyResponse;
import vn.gpg.unionportal.repository.PulseSurveyRepository;
import vn.gpg.unionportal.repository.PulseSurveyResponseRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PulseSurveyService {
    private final PulseSurveyRepository surveyRepository;
    private final PulseSurveyResponseRepository responseRepository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public PulseSurveyService(PulseSurveyRepository surveyRepository,
                              PulseSurveyResponseRepository responseRepository,
                              EntityMapper mapper,
                              CurrentUserService currentUser,
                              RealtimeEventPublisher events) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<PulseSurveyView> list(Long unitId, SurveyStatus status) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        return surveyRepository.findAll().stream()
                .filter(item -> scopedUnitId == null || item.getUnionUnit().getId().equals(scopedUnitId))
                .filter(item -> status == null || item.getStatus() == status)
                .sorted(Comparator.comparing(PulseSurvey::getStartDate).reversed())
                .map(this::toView)
                .toList();
    }

    @Transactional
    public PulseSurveyView create(PulseSurveyRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = surveyRepository.save(mapper.apply(new PulseSurvey(), request));
        events.changed("surveys", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return toView(saved);
    }

    @Transactional
    public PulseSurveyView update(Long id, PulseSurveyRequest request) {
        var survey = findById(id);
        currentUser.requireUnitAccess(survey.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = surveyRepository.save(mapper.apply(survey, request));
        events.changed("surveys", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return toView(saved);
    }

    @Transactional
    public void delete(Long id) {
        var survey = findById(id);
        currentUser.requireUnitAccess(survey.getUnionUnit().getId());
        surveyRepository.delete(survey);
        events.changed("surveys", "DELETED", survey.getId(), survey.getUnionUnit().getId());
    }

    public List<PulseSurveyResponse> responses(Long id) {
        findById(id);
        return responseRepository.findBySurveyIdOrderBySubmittedOnDesc(id);
    }

    @Transactional
    public PulseSurveyResponse respond(Long id, PulseSurveyResponseRequest request) {
        var survey = findById(id);
        currentUser.requireUnitAccess(survey.getUnionUnit().getId());
        var today = LocalDate.now();
        if (survey.getStatus() != SurveyStatus.ACTIVE) {
            throw new IllegalArgumentException("Khảo sát hiện không mở nhận phản hồi");
        }
        if (today.isBefore(survey.getStartDate()) || today.isAfter(survey.getEndDate())) {
            throw new IllegalArgumentException("Hôm nay nằm ngoài thời gian nhận phản hồi của khảo sát");
        }
        if (!request.anonymous() && (request.respondentName() == null || request.respondentName().isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập tên hoặc chọn gửi phản hồi ẩn danh");
        }

        var response = new PulseSurveyResponse();
        response.setSurvey(survey);
        response.setRating(request.rating());
        response.setNeedCategory(request.needCategory().trim());
        response.setSuggestion(trimToNull(request.suggestion()));
        response.setAnonymous(request.anonymous());
        response.setRespondentName(request.anonymous() ? null : request.respondentName().trim());
        response.setSubmittedOn(today);
        var saved = responseRepository.save(response);
        events.changed("surveys", "RESPONSE_CREATED", survey.getId(), survey.getUnionUnit().getId());
        return saved;
    }

    private PulseSurvey findById(Long id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khảo sát với id=" + id));
    }

    private PulseSurveyView toView(PulseSurvey survey) {
        long count = responseRepository.countBySurveyId(survey.getId());
        double rate = Math.round(count * 1000d / survey.getTargetResponses()) / 10d;
        return new PulseSurveyView(survey.getId(), survey.getSurveyCode(), survey.getTitle(), survey.getUnionUnit(),
                survey.getQuestionText(), survey.getStartDate(), survey.getEndDate(), survey.getStatus(),
                survey.getTargetResponses(), count, rate);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

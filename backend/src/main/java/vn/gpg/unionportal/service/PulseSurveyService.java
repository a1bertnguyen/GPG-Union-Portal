package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyResponseRequest;
import vn.gpg.unionportal.dto.ApiModels.PulseSurveyView;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.SurveyStatus;
import vn.gpg.unionportal.model.PulseSurvey;
import vn.gpg.unionportal.model.PulseSurveyResponse;
import vn.gpg.unionportal.repository.PulseSurveyRepository;
import vn.gpg.unionportal.repository.PulseSurveyResponseRepository;
import vn.gpg.unionportal.spec.PulseSurveySpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PulseSurveyService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "startDate");

    private final PulseSurveyRepository surveyRepository;
    private final PulseSurveyResponseRepository responseRepository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public PulseSurveyService(PulseSurveyRepository surveyRepository,
                              PulseSurveyResponseRepository responseRepository,
                              EntityMapper mapper,
                              CurrentUserService currentUser,
                              RealtimeEventPublisher events,
                              SpecAggregates aggregates) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<PulseSurveyView> page(ListQuery query) {
        return surveyRepository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT)).map(this::toView);
    }

    public List<PulseSurveyView> search(ListQuery query) {
        return surveyRepository.findAll(Specs.nullSafe(filter(query)), SORT).stream().map(this::toView).toList();
    }

    public ListFacets facets(ListQuery query) {
        Specification<PulseSurvey> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<PulseSurvey> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(PulseSurvey.class, filtered, Map.of(
                "active", Specs.eq("status", SurveyStatus.ACTIVE),
                "closed", Specs.eq("status", SurveyStatus.CLOSED),
                "draft", Specs.eq("status", SurveyStatus.DRAFT)));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("active", counts.value("active"));
        metrics.put("closed", counts.value("closed"));
        metrics.put("draft", counts.value("draft"));
        return new ListFacets(
                surveyRepository.count(scope),
                aggregates.distinctValues(PulseSurvey.class, scope, "status"),
                metrics);
    }

    private Specification<PulseSurvey> filter(ListQuery query) {
        return PulseSurveySpecs.filter(query, scopedUnitId(query));
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
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

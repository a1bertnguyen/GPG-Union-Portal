package vn.gpg.unionportal.service.kpi;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.repository.ActivityMediaRepository;
import vn.gpg.unionportal.repository.FinanceDocumentRepository;
import vn.gpg.unionportal.repository.WelfareDocumentRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Which source records actually have a stored file behind them.
 *
 * <p>CARE03, ACT03 and FIN01 must not treat a {@code COMPLETE} status column as proof: that column is typed
 * in by hand, the attachment is not. Lookups are batched per unit so the engine never issues one query per
 * record.
 */
@Component
@Transactional(readOnly = true)
public class KpiSourceEvidenceIndex {
    private final WelfareDocumentRepository welfareDocuments;
    private final ActivityMediaRepository activityMedia;
    private final FinanceDocumentRepository financeDocuments;

    public KpiSourceEvidenceIndex(WelfareDocumentRepository welfareDocuments,
                                  ActivityMediaRepository activityMedia,
                                  FinanceDocumentRepository financeDocuments) {
        this.welfareDocuments = welfareDocuments;
        this.activityMedia = activityMedia;
        this.financeDocuments = financeDocuments;
    }

    public Set<Long> welfareWithDocuments(Collection<Long> welfareRecordIds) {
        return present(welfareRecordIds, welfareDocuments::findWelfareRecordIdsWithDocuments);
    }

    public Set<Long> activitiesWithMedia(Collection<Long> activityIds) {
        return present(activityIds, activityMedia::findActivityIdsWithMedia);
    }

    public Set<Long> financeWithDocuments(Collection<Long> financeEntryIds) {
        return present(financeEntryIds, financeDocuments::findFinanceEntryIdsWithDocuments);
    }

    /** An empty id set never reaches the database: {@code in ()} is not valid SQL everywhere. */
    private Set<Long> present(Collection<Long> ids, Function<Collection<Long>, List<Long>> query) {
        if (ids == null || ids.isEmpty()) return Set.of();
        List<Long> found = query.apply(ids);
        return found == null || found.isEmpty() ? Set.of() : Set.copyOf(found);
    }
}

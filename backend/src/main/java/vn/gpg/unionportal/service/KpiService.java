package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.KpiModels.Dashboard;
import vn.gpg.unionportal.dto.KpiModels.Metadata;
import vn.gpg.unionportal.dto.KpiModels.PeriodType;
import vn.gpg.unionportal.service.kpi.GpgKpiEngine;

@Service
@Transactional(readOnly = true)
public class KpiService {
    private final GpgKpiEngine gpgEngine;

    public KpiService(GpgKpiEngine gpgEngine) {
        this.gpgEngine = gpgEngine;
    }

    public Dashboard evaluate(PeriodType periodType, int year, int period, Long requestedUnitId) {
        return gpgEngine.evaluate(periodType, year, period, requestedUnitId);
    }

    public Metadata metadata() {
        return gpgEngine.metadata();
    }
}

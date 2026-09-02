package vn.gpg.unionportal.repository.kpi;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.kpi.BusinessCalendarDay;

import java.time.LocalDate;
import java.util.List;

public interface BusinessCalendarDayRepository extends JpaRepository<BusinessCalendarDay, Long> {
    List<BusinessCalendarDay> findByBusinessCalendarIdAndCalendarDateBetween(
            String businessCalendarId, LocalDate from, LocalDate to);
}

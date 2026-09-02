package vn.gpg.unionportal.model.kpi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "business_calendar_days")
@Getter
@Setter
@NoArgsConstructor
public class BusinessCalendarDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_calendar_id", nullable = false, length = 60)
    private String businessCalendarId;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Column(name = "working_day", nullable = false)
    private boolean workingDay;

    @Column(length = 255)
    private String description;
}

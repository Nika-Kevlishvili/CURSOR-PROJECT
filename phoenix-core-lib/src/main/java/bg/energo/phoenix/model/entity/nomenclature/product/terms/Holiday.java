package bg.energo.phoenix.model.entity.nomenclature.product.terms;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.request.nomenclature.product.terms.HolidaysRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "holidays", schema = "nomenclature")
public class Holiday extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "holidays_id_seq",
            sequenceName = "nomenclature.holidays_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "holidays_id_seq"
    )
    private Long id;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "holiday")
    private LocalDateTime holiday;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private HolidayStatus holidayStatus;

    public Holiday(Long calendarId, HolidaysRequest request) {
        this.calendarId = calendarId;
        this.holiday = request.getHolidayDate().atStartOfDay();
        this.holidayStatus = request.getHolidayStatus();
    }
}

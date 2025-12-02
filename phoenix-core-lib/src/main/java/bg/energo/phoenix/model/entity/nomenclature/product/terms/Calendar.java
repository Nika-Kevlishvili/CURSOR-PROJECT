package bg.energo.phoenix.model.entity.nomenclature.product.terms;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.WeekDays;
import bg.energo.phoenix.model.request.nomenclature.product.terms.CalendarRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;


@Builder
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "calendars", schema = "nomenclature")
public class Calendar extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "calendars_id_seq",
            sequenceName = "nomenclature.calendars_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "calendars_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "weekends")
    private String weekends; // WeekDays enum delimited with ';' like 'SATURDAY;SUNDAY'

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    public Calendar(CalendarRequest request) {
        this.name = request.getName();

        if (request.getWeekends() != null && !request.getWeekends().isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(request.getWeekends().stream().map(WeekDays::toString).collect(Collectors.joining(";", "", "")));

            if (!stringBuilder.isEmpty()) {
                this.weekends = stringBuilder.toString();
            }
        }

        this.defaultSelection = request.getDefaultSelection();
        this.status = request.getStatus();
    }

    public void updateCalendar(CalendarRequest request) {
        this.name = request.getName();
        this.weekends = null;

        if (request.getWeekends() != null && !request.getWeekends().isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(request.getWeekends().stream().map(WeekDays::toString).collect(Collectors.joining(";", "", "")));

            if (!stringBuilder.isEmpty()) {
                this.weekends = stringBuilder.toString();
            }
        }

        this.defaultSelection = request.getDefaultSelection();
        this.status = request.getStatus();

        if (request.getStatus().equals(INACTIVE)) {
            this.defaultSelection = false;
        }
    }
}

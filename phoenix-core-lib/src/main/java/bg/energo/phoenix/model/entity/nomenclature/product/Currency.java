package bg.energo.phoenix.model.entity.nomenclature.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.currency.CurrencyRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "currencies", schema = "nomenclature")
public class Currency extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "currencies_seq",
            sequenceName = "nomenclature.currencies_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "currencies_seq"
    )
    private Long id;

    @Column(name = "name", length = 512)
    private String name;

    @Column(name = "print_name", length = 512)
    private String printName;

    @Column(name = "abbreviation", length = 128)
    private String abbreviation;

    @Column(name = "full_name", length = 512)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alt_currency_id")
    private Currency altCurrency;

    @Column(name = "alt_currency_id", insertable = false, updatable = false)
    private Long altCurrencyId;

    @Column(name = "alt_ccy_exchange_rate")
    private BigDecimal altCurrencyExchangeRate;

    @Column(name = "main_ccy")
    private Boolean mainCurrency;

    @Column(name = "main_ccy_start_date")
    private LocalDate mainCurrencyStartDate;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    public Currency(CurrencyRequest request) {
        this.name = request.getName();
        this.printName = request.getPrintName();
        this.abbreviation = request.getAbbreviation();
        this.fullName = request.getFullName();
        this.mainCurrency = request.getMainCurrency();
        this.mainCurrencyStartDate = request.getMainCurrencyStartDate();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }

    public Currency(Long id, String name, String printName, String abbreviation, String fullName, Currency altCurrency, BigDecimal altCurrencyExchangeRate, Boolean mainCurrency, LocalDate mainCurrencyStartDate, boolean defaultSelection, NomenclatureItemStatus status, Long orderingId) {
        this.id = id;
        this.name = name;
        this.printName = printName;
        this.abbreviation = abbreviation;
        this.fullName = fullName;
        this.altCurrency = altCurrency;
        this.altCurrencyExchangeRate = altCurrencyExchangeRate;
        this.mainCurrency = mainCurrency;
        this.mainCurrencyStartDate = mainCurrencyStartDate;
        this.defaultSelection = defaultSelection;
        this.status = status;
        this.orderingId = orderingId;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", printName='" + printName + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", fullName='" + fullName + '\'' +
                ", altCurrencyExchangeRate=" + altCurrencyExchangeRate +
                ", mainCurrency=" + mainCurrency +
                ", mainCurrencyStartDate=" + mainCurrencyStartDate +
                ", defaultSelection=" + defaultSelection +
                ", status=" + status +
                ", orderingId=" + orderingId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return defaultSelection == currency.defaultSelection
                && Objects.equals(id, currency.id)
                && Objects.equals(name, currency.name)
                && Objects.equals(printName, currency.printName)
                && Objects.equals(abbreviation, currency.abbreviation)
                && Objects.equals(fullName, currency.fullName)
                && Objects.equals(altCurrencyExchangeRate, currency.altCurrencyExchangeRate)
                && Objects.equals(mainCurrency, currency.mainCurrency)
                && Objects.equals(mainCurrencyStartDate, currency.mainCurrencyStartDate)
                && Objects.equals(altCurrency.getId(), currency.altCurrency.getId())
                && status == currency.status && Objects.equals(orderingId, currency.orderingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                printName,
                abbreviation,
                fullName,
                mainCurrency,
                mainCurrencyStartDate,
                defaultSelection,
                status,
                orderingId
        );
    }
}

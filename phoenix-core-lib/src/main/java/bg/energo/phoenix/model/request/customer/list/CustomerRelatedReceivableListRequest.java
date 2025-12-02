package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedReceivableSearchField;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedReceivableSortColumns;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRelatedReceivableListRequest {

    @Digits(integer = 32,fraction = 5,message = "initialAmount-[initialAmount] invalid format of amount;")
    private BigDecimal initialAmountFrom;

    @Digits(integer = 32,fraction = 5,message = "initialAmount-[initialAmount] invalid format of amount;")
    private BigDecimal initialAmountTo;

    @Digits(integer = 32,fraction = 5,message = "initialAmount-[initialAmount] invalid format of amount;")
    private BigDecimal currentAmountFrom;

    @Digits(integer = 32,fraction = 5,message = "initialAmount-[initialAmount] invalid format of amount;")
    private BigDecimal currentAmountTo;

    @Size(min = 14,max = 14,message = "billingGroup-[billingGroup] billing group length should be 14;")
    @Pattern(regexp = ".*[0-9].*",message = "billingGroup-[billingGroup] allowed symbols are 0-9")
    private String billingGroup;

    Boolean blockedForOffsetting;

    private List<Long> currencyIds;

    private String prompt;

    private CustomerRelatedReceivableSearchField customerReceivableSearchBy;

    @NotNull(message = "page-[page] page is mandatory;")
    private Integer page;

    @NotNull(message = "size-[size] size is mandatory;")
    private Integer size;

    private Sort.Direction direction;

    private CustomerRelatedReceivableSortColumns columns;

}

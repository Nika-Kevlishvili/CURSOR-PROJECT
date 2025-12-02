package bg.energo.phoenix.model.response.receivable.deposit;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Data
public class DepositCustomerResponse {

    private Long detailId;
    private Long customerId;
    private Long versionId;
    private LocalDateTime createDate;
    private String name;

    public DepositCustomerResponse(Long detailId,
                                        Long customerId,
                                        Long versionId,
                                        LocalDateTime createDate,
                                        String name,
                                        String middleName,
                                        String lastName,
                                        String legalFormName,
                                        String customerIdentifier) {
        this.detailId = detailId;
        this.customerId = customerId;
        this.versionId = versionId;
        this.createDate = createDate;
        this.name = "%s (%s%s%s%s)".formatted(
                customerIdentifier,
                name,
                StringUtils.isEmpty(middleName) ? "" : " " + middleName,
                StringUtils.isEmpty(lastName) ? "" : " " + lastName,
                StringUtils.isEmpty(legalFormName) ? "" : " " + legalFormName
        );
    }
}

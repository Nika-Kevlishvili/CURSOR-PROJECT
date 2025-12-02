package bg.energo.phoenix.model.response.contract.productContract;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Data
public class CustomerDetailsShortResponse {

    private Long detailId;
    private Long versionId;
    private LocalDateTime createDate;
    private String name;

    public CustomerDetailsShortResponse(Long detailId,
                                        Long versionId,
                                        LocalDateTime createDate,
                                        String name,
                                        String middleName,
                                        String lastName) {
        this.detailId = detailId;
        this.versionId = versionId;
        this.createDate = createDate;
        this.name = "%s%s%s".formatted(
                name,
                StringUtils.isEmpty(middleName) ? "" : " " + middleName,
                StringUtils.isEmpty(lastName) ? "" : " " + lastName
        );
    }
}

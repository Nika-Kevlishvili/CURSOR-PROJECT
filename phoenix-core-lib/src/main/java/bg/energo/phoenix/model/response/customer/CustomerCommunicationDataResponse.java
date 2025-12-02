package bg.energo.phoenix.model.response.customer;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CustomerCommunicationDataResponse {

    private Long id;
    private String name;
    private LocalDateTime createDate;
    private String concatPurposes;

    public CustomerCommunicationDataResponse(Long id,String name,LocalDateTime createDate) {
        this.id = id;
        this.name = name;
        this.createDate = createDate;
    }
}

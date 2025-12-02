package bg.energo.phoenix.model.response.crm.smsCommunication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private String name;
    boolean sendReport;
}

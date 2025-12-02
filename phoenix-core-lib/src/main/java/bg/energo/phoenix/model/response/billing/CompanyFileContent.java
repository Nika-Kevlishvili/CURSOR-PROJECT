package bg.energo.phoenix.model.response.billing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompanyFileContent {
    private String fileName;
    private byte[] content;
}

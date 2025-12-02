package bg.energo.phoenix.model.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductFileContent {
    String fileName;
    byte[] content;
}

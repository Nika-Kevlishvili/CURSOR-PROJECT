package bg.energo.phoenix.model.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductShortResponse {

    private Long id;
    private String name;
}

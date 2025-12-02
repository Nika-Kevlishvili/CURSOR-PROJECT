package bg.energo.phoenix.model.request.customer.manager;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class EditManagerRequest extends BaseManagerRequest {

    private Long id;

}

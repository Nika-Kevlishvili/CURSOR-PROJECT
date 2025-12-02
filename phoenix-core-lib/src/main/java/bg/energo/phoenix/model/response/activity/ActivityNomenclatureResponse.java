package bg.energo.phoenix.model.response.activity;

import lombok.Data;

@Data
public class ActivityNomenclatureResponse {
    private Long id;
    private String name;

    public ActivityNomenclatureResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}

package bg.energo.phoenix.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForPod extends CacheObject{

    private Integer estimatedConsumption;

    public CacheObjectForPod(Long id, String name, Integer estimatedConsumption) {
        super(id, name);
        this.estimatedConsumption = estimatedConsumption;
    }
}

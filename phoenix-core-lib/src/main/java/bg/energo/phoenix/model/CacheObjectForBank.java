package bg.energo.phoenix.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForBank extends CacheObject{

    private String bic;

    public CacheObjectForBank(Long id, String name, String bic) {
        super(id, name);
        this.bic = bic;
    }
}

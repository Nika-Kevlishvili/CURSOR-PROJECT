package bg.energo.phoenix.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForCustomer extends CacheObject{
    private Long customerNumber;

    public CacheObjectForCustomer(Long id, String name,Long customerNumber) {
        super(id, name);
        this.customerNumber=customerNumber;
    }
}

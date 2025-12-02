package bg.energo.phoenix.model;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForParent extends CacheObject{
    public CacheObjectForParent(Long id, String name, String parentName,NomenclatureItemStatus status) {
        super(id, name);
        this.status=status;
        this.parentName = parentName;
    }
    private String parentName;
    private NomenclatureItemStatus status;
}

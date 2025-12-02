package bg.energo.phoenix.model;

import jakarta.persistence.Cacheable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@AllArgsConstructor
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForDetails {
    private Long id;
    private Long versionId;
    private Long detailsId;

    public CacheObjectForDetails(Long id, Integer versionId, Long detailsId) {
        this.id = id;
        this.versionId = Long.valueOf(versionId) ;
        this.detailsId = detailsId;
    }
}

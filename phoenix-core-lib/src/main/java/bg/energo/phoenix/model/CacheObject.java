package bg.energo.phoenix.model;

import jakarta.persistence.Cacheable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObject implements Serializable {
    private Long id;
    private String name;

}

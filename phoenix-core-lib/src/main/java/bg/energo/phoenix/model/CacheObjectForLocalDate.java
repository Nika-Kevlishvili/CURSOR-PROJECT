package bg.energo.phoenix.model;

import jakarta.persistence.Cacheable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CacheObjectForLocalDate {

    private LocalDate localDate;
}

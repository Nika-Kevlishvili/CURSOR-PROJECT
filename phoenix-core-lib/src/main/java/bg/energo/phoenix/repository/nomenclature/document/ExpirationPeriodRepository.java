package bg.energo.phoenix.repository.nomenclature.document;

import bg.energo.phoenix.model.entity.nomenclature.document.DocumentExpirationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpirationPeriodRepository extends JpaRepository<DocumentExpirationPeriod, Long> {
    @Query("""
            select p
            from DocumentExpirationPeriod p
            where p.status = 'ACTIVE'
            """)
    Optional<DocumentExpirationPeriod> findActiveExpirationPeriod();
}

package phoenix.core.customer.repository.nomenclature.customer.legalForm;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalFormTransliteratedRepository extends JpaRepository<LegalFormTransliterated,Long> {
    @Query("""
        select l
        from LegalFormTransliterated l
        where l.id = :id
        and l.status in :statuses
    """)
    Optional<LegalFormTransliterated> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );
}

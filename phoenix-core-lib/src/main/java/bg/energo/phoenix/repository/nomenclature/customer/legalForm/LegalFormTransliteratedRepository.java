package bg.energo.phoenix.repository.nomenclature.customer.legalForm;


import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalFormTransliteratedRepository extends JpaRepository<LegalFormTransliterated, Long> {
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

    @Query(
    """
        select new bg.energo.phoenix.model.CacheObject(l.id, l.name)
        from LegalFormTransliterated  l
        where l.name = :name
        and l.status =:status
    """
    )
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndStatus(@Param("name") String name, @Param("status") NomenclatureItemStatus active);
}

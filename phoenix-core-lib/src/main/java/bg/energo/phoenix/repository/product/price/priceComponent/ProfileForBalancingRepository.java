package bg.energo.phoenix.repository.product.price.priceComponent;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.price.priceComponent.ProfileForBalancing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileForBalancingRepository extends JpaRepository<ProfileForBalancing, Long> {
    List<ProfileForBalancing> findAllByStatusIn(List<EntityStatus> statuses);

    @Query("""
            select pfb
            from ProfileForBalancing pfb
            where lower(pfb.name) like(:prompt)
            and pfb.status = 'ACTIVE'
            """)
    Page<ProfileForBalancing> findAllActiveBalancingProfiles(@Param("prompt") String prompt, PageRequest pageRequest);

    Optional<ProfileForBalancing> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);
}

package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.product.ProductForBalancing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductForBalancingRepository extends JpaRepository<ProductForBalancing, Long> {
    List<ProductForBalancing> findAllByStatusIn(List<EntityStatus> statuses);

    @Query("""
            select pfb
            from ProductForBalancing pfb
            where lower(pfb.name) like(:prompt)
            and pfb.status = 'ACTIVE'
            """)
    Page<ProductForBalancing> findAllActiveBalancingProducts(@Param("prompt") String prompt, PageRequest pageRequest);

    Optional<ProductForBalancing> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);
}

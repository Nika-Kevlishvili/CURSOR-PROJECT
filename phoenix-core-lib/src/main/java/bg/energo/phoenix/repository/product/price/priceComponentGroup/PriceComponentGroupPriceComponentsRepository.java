package bg.energo.phoenix.repository.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupPriceComponent;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupPriceComponentStatus;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupPriceComponentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceComponentGroupPriceComponentsRepository extends JpaRepository<PriceComponentGroupPriceComponent, Long> {

    @Query(
            value = """
                        select pcgpc from PriceComponentGroupPriceComponent pcgpc
                        join PriceComponentGroupDetails pcgd on pcgd.id = pcgpc.priceComponentGroupDetailId
                            where pcgd.priceComponentGroupId = :priceComponentGroupId
                            and pcgd.versionId = :priceComponentGroupVersion
                            and pcgpc.status in (:statuses)
                    """
    )
    List<PriceComponentGroupPriceComponent> findByPriceComponentGroupVersionAndStatusIn(
            @Param("priceComponentGroupId") Long priceComponentGroupId,
            @Param("priceComponentGroupVersion") Long priceComponentGroupVersion,
            @Param("statuses") List<PriceComponentGroupPriceComponentStatus> statuses
    );

    boolean existsByPriceComponentIdAndStatus(Long priceComponentId, PriceComponentGroupPriceComponentStatus status);

    @Query(value =
            """
                    select new bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupPriceComponentResponse(
                        pcgpc.id,
                        pcgpc.priceComponentId,
                        pc.name
                    ) 
                    from PriceComponentGroupPriceComponent pcgpc
                    join PriceComponent pc on pc.id = pcgpc.priceComponentId
                        where pcgpc.priceComponentGroupDetailId = :priceComponentGroupDetailId
                        and pcgpc.status in (:statuses)
                    """
    )
    List<PriceComponentGroupPriceComponentResponse> findByPriceComponentGroupDetailIdAndStatusIn(
            @Param("priceComponentGroupDetailId") Long priceComponentGroupDetailId,
            @Param("statuses") List<PriceComponentGroupPriceComponentStatus> statuses
    );


/*    @Query("""
            select pcgpc from PriceComponentGroupPriceComponent pcgpc
            where pcgpc.priceComponentId = :priceComponentId
            """)*/
    List<PriceComponentGroupPriceComponent> findByPriceComponentIdAndStatusIn(Long priceComponentId, List<PriceComponentGroupPriceComponentStatus> statuses);

    List<PriceComponentGroupPriceComponent> findByPriceComponentGroupDetailIdAndStatus(Long priceComponentGroupDetailId, PriceComponentGroupPriceComponentStatus statuses);

}

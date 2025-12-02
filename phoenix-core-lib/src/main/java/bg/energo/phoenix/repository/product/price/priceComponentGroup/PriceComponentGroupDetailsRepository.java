package bg.energo.phoenix.repository.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceComponentGroupDetailsRepository extends JpaRepository<PriceComponentGroupDetails, Long> {

    Optional<PriceComponentGroupDetails> findFirstByPriceComponentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(Long priceComponentGroupId, LocalDate startDate);

    Optional<PriceComponentGroupDetails> findByPriceComponentGroupIdAndVersionId(Long priceComponentGroupId, Long versionId);

    boolean existsByPriceComponentGroupIdAndStartDate(Long priceComponentGroupId, LocalDate startDate);

    @Query("select max(pcgd.versionId) from PriceComponentGroupDetails pcgd where pcgd.priceComponentGroupId = :priceComponentGroupId")
    Long findLastVersionByPriceComponentGroupId(@Param("priceComponentGroupId") Long priceComponentGroupId);

    @Query(value =
            """
                    select new bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupVersion(
                        pcgd.versionId,
                        pcgd.startDate,
                        pcgd.endDate
                    )
                    from PriceComponentGroupDetails pcgd
                        where pcgd.priceComponentGroupId = :priceComponentGroupId
                        order by pcgd.startDate ASC 
                    """
    )
    List<PriceComponentGroupVersion> getPriceComponentGroupVersions(
            @Param("priceComponentGroupId") Long priceComponentGroupId
    );

    List<PriceComponentGroupDetails> findByPriceComponentGroupId(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(pcgd.id,pcgd.versionId,pcgd.startDate)
            from PriceComponentGroupDetails pcgd
            where pcgd.priceComponentGroupId =:id
            order by pcgd.startDate ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyGroupBaseRequest(@Param("id") Long groupId);
}

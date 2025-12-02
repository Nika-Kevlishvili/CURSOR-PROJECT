package bg.energo.phoenix.repository.product.price.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetails;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterDetailsVersionInfo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceParameterDetailsRepository extends JpaRepository<PriceParameterDetails, Long> {

    @Query("""
                select ppd
                from PriceParameterDetails as ppd
                where ppd.priceParameterId = :priceParameterId
                and ppd.versionId =:versionId
            """)
    Optional<PriceParameterDetails> findByPriceParameterIdAndVersionId(
            @Param("priceParameterId") Long priceParameterId ,
            @Param("versionId") Long versionId
    );

    @Query(value = """
                select ppd from PriceParameterDetails as ppd
                left join PriceParameter pp on ppd.priceParameterId = pp.id
                    where ppd.name = :name
                    and pp.status in (:statuses)
            """)
    List<PriceParameterDetails> findAllByNameAndPriceParameterStatus(
            @Param("name") String name,
            @Param("statuses") List<PriceParameterStatus> statuses
    );

    Optional<PriceParameterDetails> findFirstByPriceParameterId(Long id, Sort version);

    @Query("""
            select new bg.energo.phoenix.model.response.priceParameter.PriceParameterDetailsVersionInfo(ppd.id, concat(ppd.versionId, ' / ', ppd.name),ppd.versionId, ppd.createDate)
            from PriceParameterDetails ppd
            where ppd.priceParameterId = :priceParameterId
            """)
    List<PriceParameterDetailsVersionInfo> getPriceParameterExistingVersions(Long priceParameterId);

    @Query("""
                select ppd from PriceParameterDetails as ppd
                left join PriceParameter pp on ppd.priceParameterId = pp.id
                    where ppd.name = :name
                    and pp.status in (:statuses)
                    and pp.id <> :parameterId
""")
    List<PriceParameterDetails> findByNameAndNotParameterId(@Param("name")String name,@Param("parameterId") Long parameterId,@Param("statuses") List<PriceParameterStatus> statuses);
@Query
        ("select max(ppd.versionId) from PriceParameterDetails as ppd where ppd.priceParameterId = :id")
    Long findLatestByParameterId(Long id);


    @Query(
            value = """
                    select ppd.name
                        from PriceParameter pp
                        join PriceParameterDetails ppd on pp.lastPriceParameterDetailId = ppd.id
                            where pp.id = :id
                    """
    )
    String findNameOfLatestVersionByParameterId(Long id);

}


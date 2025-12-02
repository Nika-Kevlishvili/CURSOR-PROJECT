package bg.energo.phoenix.repository.pod.billingByScales;

import bg.energo.phoenix.model.entity.pod.billingByScale.BillingDataByScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface BillingDataByScaleRepository extends JpaRepository<BillingDataByScale, Long> {

    @Modifying
    @Transactional
    @Query("delete from BillingDataByScale bdbs where bdbs.billingByScaleId = :id and  bdbs.id not in (:ids)")
    void deleteByIdNotIn(Long id, List<Long> ids);

    List<BillingDataByScale> findByBillingByScaleId(Long id);

    List<BillingDataByScale> findByBillingByScaleIdOrderByIndexAsc(Long id);

    @Query("""
            Select bdbs from BillingDataByScale bdbs
            inner join Scales s on s.id = bdbs.scaleId
            where bdbs.periodFrom = :periodFrom and bdbs.billingByScaleId = :billingByScaleId and bdbs.meterId = :meterId and s.scaleCode = :scaleCode
            """)
    BillingDataByScale findByPeriodFromAndBillingByScaleIdAndMeterId(LocalDate periodFrom, Long billingByScaleId, Long meterId,String scaleCode);
    @Query("""
            Select bdbs from BillingDataByScale bdbs
            inner join Scales s on s.id = bdbs.scaleId
            where bdbs.periodFrom = :periodFrom and bdbs.billingByScaleId = :billingByScaleId and (:meterId is null or bdbs.meterId = :meterId) and s.tariffScale = :tariffScale
            """)
    BillingDataByScale findByPeriodFromAndBillingByScaleIdAndMeterIdTariffScale(LocalDate periodFrom, Long billingByScaleId, Long meterId,String tariffScale);

    Boolean existsByScaleId(Long scaleId);


    //Todo for testing purposes will be deleted in future.
    @Query("""
            select bdbs from BillingDataByScale bdbs
            join Scales s on s.id=bdbs.scaleId
            where bdbs.billingByScaleId =:id
            and s.scaleForActiveElectricity = true
            """)
    List<BillingDataByScale> findByBillingByScaleIdWithoutActiveElectricity(Long id);
}

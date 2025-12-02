package bg.energo.phoenix.repository.product.price.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterDetailInfoPreviewResponse;
import bg.energo.phoenix.model.response.priceParameter.ServiceOrderProcessPriceParamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PriceParameterDetailInfoRepository extends JpaRepository<PriceParameterDetailInfo, Long> {


    @Query("""
                select ppdi
                from PriceParameterDetailInfo as ppdi
                where ppdi.priceParameterDetailId = :priceParameterDetailsId
                and ppdi.periodFrom =:periodFrom
                and ppdi.periodTo =:periodTo
                and ppdi.isShiftedHour =:isShiftedHour
            """)
    Optional<PriceParameterDetailInfo> findByPriceParameterDetailsIdAndPeriodAndIsShiftedHour(@Param("priceParameterDetailsId") Long priceParameterDetailsId,
                                                                                              @Param("periodFrom") LocalDateTime periodFrom,
                                                                                              @Param("periodTo") LocalDateTime periodTo,
                                                                                              @Param("isShiftedHour") Boolean isShiftedHour);

    @Query("""
            select new bg.energo.phoenix.model.response.priceParameter.PriceParameterDetailInfoPreviewResponse(ppdi) from PriceParameterDetailInfo ppdi
            join PriceParameterDetails ppd on ppd.id = ppdi.priceParameterDetailId
            join PriceParameter pp on ppd.priceParameterId = pp.id
            where ppdi.periodFrom >= :periodFrom
            and ppdi.periodFrom <= :periodTo
            and ppd.priceParameterId = :id
            and ppd.versionId = :version
            order by ppdi.periodFrom
            """)
    List<PriceParameterDetailInfoPreviewResponse> findByPeriodFromAndPeriodTo(@Param("id") Long id,
                                                                              @Param("version") Long version,
                                                                              @Param("periodFrom") LocalDateTime periodFrom,
                                                                              @Param("periodTo") LocalDateTime periodTo);

    @Query("""
                select p
                from PriceParameterDetailInfo as p
                where p.priceParameterDetailId = :detailId
                and p.periodFrom =:periodFrom
                and p.isShiftedHour =:isShiftedHour
            """)
    Optional<PriceParameterDetailInfo> findByDetailIdAndPeriodAndIsShifted(@Param("detailId") Long detailId, @Param("periodFrom") LocalDateTime periodFrom, @Param("isShiftedHour") Boolean isShiftedHour);

    List<PriceParameterDetailInfo> findAllByPriceParameterDetailId(Long detailId);

    @Query(nativeQuery = true, value = """
        select *
        from prices.price_parameter_detail_info ppdi
        where ppdi.price_parameter_detail_id = :detailId
          and ppdi.period_from >= :periodFrom
          and ppdi.period_from <= :periodTo
        order by ppdi.period_from asc
    """)
    List<PriceParameterDetailInfo> findPriceParameterDetailInfoWithinPeriod(Long detailId, LocalDateTime periodFrom, LocalDateTime periodTo);

    @Query(value = """
            select ppdi.price       as price
                  from prices.price_parameters pp
                           join prices.price_parameter_details ppd on pp.last_price_parameter_detail_id = ppd.id
                           join prices.price_parameter_detail_info ppdi on ppdi.price_parameter_detail_id = ppd.id
                  where pp.id = :priceParameterId
                    and (
                          (
                            cast(:dateFrom as timestamp without time zone) >= (
                                case when :billingByProfileTimeZone = 'EET' and pp.time_zone = 'CET'
                                    then (cast(ppdi.period_from as timestamp without time zone) + interval '1 hour')
                                    when :billingByProfileTimeZone = 'CET' and pp.time_zone = 'EET'
                                    then (cast(ppdi.period_from as timestamp without time zone) - interval '1 hour')
                                    else cast(ppdi.period_from as timestamp without time zone)
                                end
                            )
                            and cast(:dateFrom as timestamp without time zone) < (
                                case when :billingByProfileTimeZone = 'EET' and pp.time_zone = 'CET'
                                    then (cast(ppdi.period_to as timestamp without time zone) + interval '1 hour')
                                    when :billingByProfileTimeZone = 'CET' and pp.time_zone = 'EET'
                                    then (cast(ppdi.period_to as timestamp without time zone) - interval '1 hour')
                                    else cast(ppdi.period_to as timestamp without time zone)
                                end
                            )
                          )
                          and pp.status = 'ACTIVE'
                          and ppdi.is_shifted_hour = :isShiftedHour
                          and text(pp.period_type) in (:dimensions)
                      )
            """, nativeQuery = true)
    Optional<BigDecimal> findPriceParameterLastDetailPriceInDateRangeByPriceParameterId(Long priceParameterId,
                                                                                        LocalDateTime dateFrom,
                                                                                        boolean isShiftedHour,
                                                                                        String billingByProfileTimeZone,
                                                                                        List<String> dimensions);


    @Query(value = "select pi from PriceParameterDetailInfo pi" +
                   " inner join PriceParameterDetails d on pi.priceParameterDetailId = d.id" +
                   " inner join PriceParameter p on d.priceParameterId = p.id" +
                   " where p.id =:priceParameterId and pi.periodFrom =:date and p.periodType =:type ")
    PriceParameterDetailInfo findByParameterIdAndTypeAndExactDate(Long priceParameterId,
                                                                  LocalDateTime date,
                                                                  PeriodType type);

    @Query(value = "select new bg.energo.phoenix.model.response.priceParameter.ServiceOrderProcessPriceParamResponse(" +
                   "p.id," +
                   "pi.price," +
                   "p.periodType" +
                   ")" +
                   " from PriceParameterDetailInfo pi" +
                   " inner join PriceParameterDetails d on pi.priceParameterDetailId = d.id" +
                   " inner join PriceParameter p on d.priceParameterId = p.id" +
                   " where p.id in (:priceParameterIds)" +
                   "and ((p.periodType ='ONE_MONTH' and pi.periodFrom = function('date_trunc', 'month', current_date))" +
                   "or (p.periodType = 'ONE_DAY' and pi.periodFrom = function('date_trunc', 'day', current_date))" +
                   "or (p.periodType = 'ONE_HOUR' or p.periodType = 'FIFTEEN_MINUTES'))")
    List<ServiceOrderProcessPriceParamResponse> findByParameterIdsAndTypeAndExactDate(Set<Long> priceParameterIds);

}

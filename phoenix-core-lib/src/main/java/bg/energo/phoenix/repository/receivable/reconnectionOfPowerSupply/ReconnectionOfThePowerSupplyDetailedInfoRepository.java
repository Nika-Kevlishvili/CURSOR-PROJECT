package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentInfoResponse;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyPods;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ReconnectionOfThePowerSupplyDetailedInfoRepository extends JpaRepository<ReconnectionOfThePowerSupplyPods, Long> {


    @Query(nativeQuery = true, value = """
        select rp.id,
               rp.cancelation_reason_id,
               rp.create_date,
               rp.customer_id,
               rp.modify_date,
               rp.modify_system_user_id,
               rp.pod_id,
               rp.power_supply_disconnection_request_id,
               rp.power_supply_reconnection_id,
               rp.reconnection_date,
               rp.system_user_id
        from receivable.power_supply_reconnections r
                 join receivable.power_supply_reconnection_pods rp
                      on rp.power_supply_reconnection_id = r.id
        where r.id = :reconnectionId
          and not exists(select r2.id
                         from receivable.power_supply_reconnections r2
                                  join receivable.power_supply_reconnection_pods rp2
                                       on r2.id = :reconnectionId
                         where r2.reconnection_status = 'EXECUTED'
                           and rp2.pod_id = rp.id
                           and rp2.customer_id = rp.customer_id)
    """)
    Set<ReconnectionOfThePowerSupplyPods> findByReconnectionId(@Param("reconnectionId") Long reconnectionId);

    @Query("""
                    select pod,r from ReconnectionOfThePowerSupplyPods r
                    join PointOfDelivery pod on r.podId=pod.id
                    where pod.identifier in :podIdentifier
                    and r.powerSupplyReconnectionId=:reconnectionId
            """)
    List<Object[]> findByPodIdentifiersAndReconnectionId(Set<String> podIdentifier, Long reconnectionId);

    @Query("""
            select rtpsp.podId
            from ReconnectionOfThePowerSupplyPods rtpsp
            where rtpsp.powerSupplyReconnectionId=:reconnectionId
            """)
    List<Long> findPodIdsByReconnectionId(Long reconnectionId);

    @Query(value = """
            SELECT p.identifier as PODIdentifier,
                   result.customer_id as CustomerNumber,
                   c.identifier as CustomerIdentifier,
                   CASE
                       WHEN c.customer_type = 'PRIVATE_CUSTOMER'
                           THEN concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                       ELSE concat(cd.name, ' ', lf.name)
                       END as CustomerNameComb,
                   cd.name as CustomerName,
                   cd.middle_name as CustomerMiddleName,
                   cd.last_name as CustomerSurname,
                   pd.measurement_type as MeasurementType,
                   dr.name as Reason,
                   result.reconnection_date as ReconnectionDate,
                   concat_ws(
                           nullif(concat_ws(' ',
                                            case
                                                when pd.foreign_address is true then concat(pd.district_foreign, ',')
                                                else concat(d.name, ',')
                                                end,
                                            case
                                                when pd.foreign_address is true then
                                                    case
                                                        when cd.foreign_residential_area_type is not null
                                                            then concat(pd.foreign_residential_area_type, ' ', pd.residential_area_foreign)
                                                        else pd.residential_area_foreign
                                                        end
                                                else
                                                    case
                                                        when ra.type is not null
                                                            then concat(ra.type, ' ', ra.name)
                                                        else ra.name
                                                        end
                                                end
                                  ), ''),
                           nullif(concat_ws(' ',
                                            case when pd.foreign_address is true then pd.foreign_street_type else s.type end,
                                            case when pd.foreign_address is true then pd.street_foreign else s.name end,
                                            cd.street_number
                                  ), ''),
                           nullif(concat('бл. ', pd.block), 'бл. '),
                           nullif(concat('вх. ', pd.entrance), 'вх. '),
                           nullif(concat('ет. ', pd.floor), 'ет. '),
                           nullif(concat('ап. ', pd.apartment), 'ап. '),
                           nullif(pd.address_additional_info, '')
                   ) as PODAddressComb
            FROM receivable.power_supply_reconnections psdr
                     JOIN receivable.power_supply_reconnection_pods result ON result.power_supply_reconnection_id = psdr.id
                     JOIN customer.customers c ON c.id = result.customer_id
                     JOIN customer.customer_details cd ON cd.id = c.last_customer_detail_id
                     JOIN pod.pod p ON p.id = result.pod_id
                     JOIN pod.pod_details pd ON pd.id = p.last_pod_detail_id
                     JOIN nomenclature.cancelation_reasons dr ON result.cancelation_reason_id = dr.id
                     LEFT JOIN nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                     LEFT JOIN nomenclature.districts d on pd.district_id = d.id
                     LEFT JOIN nomenclature.residential_areas ra on pd.residential_area_id = ra.id
                     LEFT JOIN nomenclature.streets s on pd.street_id = s.id
            WHERE psdr.id = :requestId
            """, nativeQuery = true)
    List<DisconnectionPowerSupplyRequestsDocumentInfoResponse> getPodImplForDocument(@Param("requestId") Long requestId);
}

package phoenix.core.customer.repository.customer.communicationData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.communicationData.ForeignAddressInfo;
import phoenix.core.customer.model.response.customer.communicationData.LocalAddressInfo;

import java.util.List;
import java.util.Optional;

public interface CustomerCommunicationsRepository extends JpaRepository<CustomerCommunications, Long> {

    @Query(
        """
            select cc from CustomerCommunications as cc
                where cc.id = :id
                and cc.status in :statuses
        """
    )
    Optional<CustomerCommunications> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select new phoenix.core.customer.model.response.customer.communicationData.LocalAddressInfo(
                c.id,
                c.name,
                r.id,
                r.name,
                m.id,
                m.name,
                pp.id,
                pp.name,
                zc.id,
                zc.name,
                d.id,
                d.name,
                ra.id,
                ra.name,
                s.id,
                s.name
            )
            from CustomerCommunications as cc
            left join PopulatedPlace as pp on pp.id = cc.populatedPlaceId
            left join Municipality as m on m.id = pp.municipality.id
            left join Region as r on r.id = m.region.id
            left join Country as c on c.id = cc.countryId
            left join ZipCode as zc on zc.id = cc.zipCodeId
            left join District as d on d.id = cc.districtId
            left join ResidentialArea as ra on ra.id = cc.residentialAreaId
            left join Street as s on s.id = cc.streetId
                where cc.id = :id
        """
    )
    LocalAddressInfo getLocalAddressInfo(
            @Param("id") Long id
    );

    @Query(
        """
            select new phoenix.core.customer.model.response.customer.communicationData.ForeignAddressInfo(
                c.id,
                c.name,
                cc.regionForeign,
                cc.municipalityForeign,
                cc.populatedPlaceForeign,
                cc.zipCodeForeign,
                cc.districtForeign,
                cc.residentialAreaForeign,
                cc.streetForeign
            )
            from CustomerCommunications as cc
            left join Country as c on c.id = cc.countryId
                where cc.id = :id
        """
    )
    ForeignAddressInfo getForeignAddressInfo(
            @Param("id") Long id
    );

    @Query(
        """
            select cc from CustomerCommunications as cc
                where cc.customerDetailsId = :customerDetailId
                and cc.status in :statuses
        """
    )
    List<CustomerCommunications> findByCustomerDetailIdAndStatuses(
            @Param("customerDetailId") Long customerDetailId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select cc from CustomerCommunications as cc
                where cc.customerDetailsId = :customerDetailId
                and cc.status in :statuses
        """
    )
    List<CustomerCommunications> getCustomerCommunicationIdsByCustomerDetailId(
            @Param("customerDetailId") Long id, 
            @Param("statuses") List<Status> statuses
    );
}

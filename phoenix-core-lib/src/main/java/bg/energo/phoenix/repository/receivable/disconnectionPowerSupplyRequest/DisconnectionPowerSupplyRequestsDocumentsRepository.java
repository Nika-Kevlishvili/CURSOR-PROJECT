package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DisconnectionPowerSupplyRequestsDocumentsRepository extends JpaRepository<DisconnectionPowerSupplyRequestsDocuments, Long> {

    @Query(value = """
            select dpsrd.id + 1
            from receivable.disconnection_power_supply_requests_documents dpsrd
            order by dpsrd.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}

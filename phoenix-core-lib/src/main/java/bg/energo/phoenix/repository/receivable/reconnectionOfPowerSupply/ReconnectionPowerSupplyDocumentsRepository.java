package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconnectionPowerSupplyDocumentsRepository extends JpaRepository<ReconnectionOfThePowerSupplyDocuments, Long> {

    @Query(value = """
            select rpsd.id + 1
            from receivable.reconnection_power_supply_documents rpsd
            order by rpsd.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}

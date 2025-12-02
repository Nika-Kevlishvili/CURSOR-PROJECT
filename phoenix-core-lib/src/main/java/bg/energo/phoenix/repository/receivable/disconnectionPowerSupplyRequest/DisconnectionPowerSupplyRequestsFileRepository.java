package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsFile;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyRequestsFileRepository extends JpaRepository<DisconnectionPowerSupplyRequestsFile, Long> {

    Optional<List<DisconnectionPowerSupplyRequestsFile>> findByPowerSupplyDisconnectionRequestIdAndStatus(Long id, EntityStatus status);

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(
                            dp.id,
                            dp.name
                    )
                    from DisconnectionPowerSupplyRequestsFile dp
                    where dp.powerSupplyDisconnectionRequestId = :id 
                    and dp.status = 'ACTIVE'
                                   """
    )
    List<ShortResponse> getFilesResponseForPreview(Long id);

}

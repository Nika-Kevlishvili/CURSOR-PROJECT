package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyCommunicationAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyCommunicationAddressRepository extends JpaRepository<CompanyCommunicationAddress, Long> {

    List<CompanyCommunicationAddress> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

}

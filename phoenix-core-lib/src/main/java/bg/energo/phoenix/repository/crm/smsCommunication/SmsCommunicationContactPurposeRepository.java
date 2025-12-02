package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationContactPurpose;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SmsCommunicationContactPurposeRepository extends JpaRepository<SmsCommunicationContactPurpose,Long> {

    @Query("""
          select new bg.energo.phoenix.model.response.shared.ShortResponse(
                cp.id,
                cp.name
            )
          from SmsCommunicationContactPurpose scp
          join ContactPurpose cp on scp.contactPurposeId=cp.id
          where scp.smsCommunicationId=:smsCommunicationId
          and scp.status='ACTIVE'
   """)
    List<ShortResponse> findContactPurposesBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        select scp from SmsCommunicationContactPurpose scp
        where scp.status='ACTIVE'
        and scp.smsCommunicationId=:smsCommunicationId
""")
    Set<SmsCommunicationContactPurpose> findContactPurposeBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        select cp.id from SmsCommunicationContactPurpose scp
        join ContactPurpose cp on scp.contactPurposeId=cp.id
        where scp.status='ACTIVE'
        and scp.smsCommunicationId=:smsCommunicationId
""")
    Set<Long> findContactPurposeIdsBySmsCommunicationId(Long smsCommunicationId);
}

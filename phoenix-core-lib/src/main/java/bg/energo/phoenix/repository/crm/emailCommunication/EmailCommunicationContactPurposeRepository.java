package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationContactPurpose;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailCommunicationContactPurposeRepository extends JpaRepository<EmailCommunicationContactPurpose, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                  cp.id,
                  cp.name
            )
            from EmailCommunicationContactPurpose scp
            join ContactPurpose cp on scp.contactPurposeId=cp.id
            where scp.emailCommunicationId=:emailCommunicationId
            and scp.status='ACTIVE'
             """
    )
    List<ShortResponse> findContactPurposesByEmailCommunicationId(Long emailCommunicationId);

    @Query(value = """
                    select eccp
                    from EmailCommunicationContactPurpose eccp
                    where eccp.emailCommunicationId = :emailCommunicationId
                    and eccp.status = 'ACTIVE'
            """
    )
    List<EmailCommunicationContactPurpose> findAllActiveContactPurposeByEmailCommunicationId(Long emailCommunicationId);
}

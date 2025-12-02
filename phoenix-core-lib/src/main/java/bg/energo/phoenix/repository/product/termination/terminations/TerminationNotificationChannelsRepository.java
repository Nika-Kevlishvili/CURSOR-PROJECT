package bg.energo.phoenix.repository.product.termination.terminations;

import bg.energo.phoenix.model.entity.product.termination.terminations.TerminationNotificationChannel;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationNotificationChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerminationNotificationChannelsRepository extends JpaRepository<TerminationNotificationChannel, Long> {

    List<TerminationNotificationChannel> findByTerminationId(Long terminationId);


    @Query(
            value = """
                    select tnch.terminationNotificationChannelType
                        from TerminationNotificationChannel tnch
                        where tnch.termination.id = :terminationId
                    """
    )
    List<TerminationNotificationChannelType> getTerminationNotificationChannelsByTerminationId(Long terminationId);

}

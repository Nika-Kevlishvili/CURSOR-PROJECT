package bg.energo.phoenix.repository.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.receivable.collectionChannel.PaymentFTPFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentFTPFilesRepository extends JpaRepository<PaymentFTPFiles,Long> {
    boolean existsByName(String name);
}

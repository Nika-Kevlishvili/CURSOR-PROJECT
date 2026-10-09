package bg.energo.phoenix.virtualpos.repository;

import bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualPosVerifyDetailsRepository extends JpaRepository<VirtualPosVerifyDetailsEntity, Long> {
}



package bg.energo.phoenix.virtualpos.repository;

import bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualPosVerifyRepository extends JpaRepository<VirtualPosVerifyEntity, Long> {
}



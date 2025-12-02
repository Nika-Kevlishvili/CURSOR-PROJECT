package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectionToChangeOfCbgFileRepository extends JpaRepository<ObjectionToChangeOfCbgFile, Long> {

    Optional<ObjectionToChangeOfCbgFile> findByIdAndStatus(Long id, EntityStatus status);

    @Query("""
        select cbgf.id
        from ObjectionToChangeOfCbgFile cbgf
        where cbgf.changeOfCbg = :cbgId
    """)
    List<Long> findFileIdsByCbgId(@Param("cbgId") Long cbgId);

}

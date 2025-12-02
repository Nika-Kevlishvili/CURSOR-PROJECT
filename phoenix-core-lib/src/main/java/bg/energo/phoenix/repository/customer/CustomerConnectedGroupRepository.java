package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerConnectedGroup;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.ConnectedGroupCustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerConnectedGroupRepository extends JpaRepository<CustomerConnectedGroup, Long> {
    @Query("""
             select new bg.energo.phoenix.model.response.customer.ConnectedGroupCustomerResponse(
             g.customerId,
             c.identifier,
             c.customerType,
             cd.name,
             cd.middleName,
             cd.lastName,
             lf.name
             )
             from CustomerConnectedGroup g
             join Customer c on c.id = g.customerId
             join CustomerDetails cd on cd.customerId = c.id
             left join LegalForm lf on lf.id=cd.legalFormId
             where g.connectedGroupId = :id
             and g.status in (:statuses)
             and cd.versionId = (select max(cds.versionId)from CustomerDetails cds where cds.customerId=g.customerId)
             """)
    List<ConnectedGroupCustomerResponse> findCustomerInfoByGroupId(@Param("id") Long id, @Param("statuses")List<Status> statuses);
    @Query("""
            select g from CustomerConnectedGroup g
            where g.connectedGroupId = :id
            and g.status in (:statuses)
            """)
    List<CustomerConnectedGroup> findByConnectedGroupId(@Param("id") Long id, @Param("statuses")List<Status> statuses);
}

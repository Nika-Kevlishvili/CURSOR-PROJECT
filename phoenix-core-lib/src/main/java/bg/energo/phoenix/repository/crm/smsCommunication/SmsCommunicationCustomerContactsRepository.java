package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomerContacts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsCommunicationCustomerContactsRepository extends JpaRepository<SmsCommunicationCustomerContacts,Long> {
    @Modifying
    void deleteAllBySmsCommunicationCustomerId(Long emailCommunicationCustomerId);

    @Query(
            value = """
                    select eccc
                    from SmsCommunicationCustomerContacts eccc
                    join SmsCommunicationCustomers ecc on ecc.id = eccc.smsCommunicationCustomerId
                    where ecc.smsCommunicationId = :smsCommunicationId
                    """
    )

    Optional<SmsCommunicationCustomerContacts> getContactBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        select sccc.phoneNumber from SmsCommunicationCustomers scc
        join SmsCommunicationCustomerContacts sccc on sccc.smsCommunicationCustomerId = scc.id
        where scc.id=:id
""")
    String getPhoneNumberBySmsCommunicationCustomerId(Long id);



}

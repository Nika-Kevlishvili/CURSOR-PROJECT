package bg.energo.phoenix.repository.customer.indicators;

import bg.energo.phoenix.service.customer.indicators.EnergoProDatabaseConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnergoProRepository {
    private final EnergoProDatabaseConnectionService databaseConnectionService;


    public int getCustomerLawsuitsCount(String customerIdentifier) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            CallableStatement callableStatement = connection.prepareCall(
                    "{call P_FTS_CUSTOMER_CASES_INFO(?)}");

            callableStatement.setString(1, customerIdentifier);
            boolean hasResults = callableStatement.execute();

            int rowCount = 0;
            if (hasResults) {
                ResultSet rs = callableStatement.getResultSet();
                while (rs.next()) {
                    rowCount++;
                }
            }

            return rowCount;
        }
    }
}

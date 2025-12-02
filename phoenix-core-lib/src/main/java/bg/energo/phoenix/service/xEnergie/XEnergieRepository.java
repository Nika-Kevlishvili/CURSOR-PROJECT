package bg.energo.phoenix.service.xEnergie;

import bg.energo.phoenix.exception.XEnergieException;
import bg.energo.phoenix.model.enums.communication.xEnergie.XEnergieRepositoryCreateCustomerResponse;
import bg.energo.phoenix.model.response.communication.xEnergie.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class XEnergieRepository {
    private final XEnergieDatabaseConnectionService databaseConnectionService;

    /**
     * Checking for customer existence in xEnergie
     *
     * @param customerIdentifier - Customer Identifier
     * @param customerNumber     - Customer Number
     *                           <p>dic - Customer Identifier </p>
     *                           <p>ico - Customer Number</p>
     * @return true - if customer exists in xEnergie
     * @throws Exception - if exception handled while trying to fetch customer
     */
    public boolean isCustomerExists(String customerIdentifier, String customerNumber) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select count(id) from hdo.cm2_customer
                            where dic = ?
                            and ico = ?
                            """
            );

            preparedStatement.setString(1, customerIdentifier);
            preparedStatement.setString(2, customerNumber);

            ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.next();

            int count = resultSet.getInt(1);

            return count > 0;
        }
    }

    public AdditionalInformationForPointOfDeliveries retrieveAdditionalInformationForPointOfDelivery(String podIdentifier) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            SELECT * FROM (SELECT DISTINCT
                                               TRUNC(hdo.time2date(o.CAS_OD, 1))   AS "date_from",
                                               TRUNC(hdo.time2date(o.CAS_DO - 7200, 1)) AS "date_to",
                                               c.NAME                               AS "owner",
                                               c.EAN                                AS "PDT_owner",
                                               o.EAN                                AS "EAN/EIC",
                                               case when (c.dic = c4.dic or k.class = 2) then oadeal.VAL_INT
                                                    else null end                     AS "deal_id",
                                               o.NAZOV                              AS "name",
                                               o.POPIS                              AS "description",
                                               o.TYP_OPM                            AS "type",
                                               o.NAP_UROVEN                         AS "volt_level_1",
                                               o.TYP_MER                            AS "measurement_type",
                                               o.TYP_ZDROJ                          AS "source_type",
                                               '000' || to_char(o.ID_SIET)          AS "distribution_network",
                                               hdo.OPM_SIET.NAZOV                       AS "network",
                                               c2.NAME                              AS "supplier",
                                               o.ID_OBLAST_TDD                      AS "LP_region1",
                                               c3.NAME                              AS "coordinator_of_balance_group",
                                               oamsr.VAL_STR                        AS "measurement_system",
                                               asp_cust.ean || ' ' || asp_cust.name as "ancillary_services_provider",
                                               agg_gr.ean                as "metering_data_provider"
                                           FROM hdo.opm o
                                                    LEFT JOIN hdo.CM2_CUSTOMER c
                                                              ON o.ID_OWNER = c.ID
                                                    LEFT JOIN hdo.OPM_ATTR oadeal
                                                              ON o.ID_OPM = oadeal.ID_OPM
                                                    INNER JOIN hdo.OPM_SIET
                                                               ON o.ID_SIET = OPM_SIET.ID_SIET
                                                    LEFT JOIN hdo.CM2_CUSTOMER c2
                                                              ON o.ID_DOD = c2.ID
                                                    LEFT JOIN hdo.OPM_TDD_OBLAST
                                                              ON o.ID_OBLAST_TDD = OPM_TDD_OBLAST.ID
                                                    LEFT JOIN hdo.OPM_ATTR oamsr
                                                              ON o.ID_OPM = oamsr.ID_OPM
                                                    LEFT JOIN hdo.CM2_CUSTOMER c3
                                                              ON o.ID_SUBJEKT_ZUCT    = c3.ID
                                                    LEFT JOIN hdo.cm2_customer asp_cust on asp_cust.id = o.id_posk_pps
                                                    left join hdo.cm2_kontrakt k on k.cislo = oadeal.VAL_INT
                                                    left join hdo.CM2_CUSTOMER c4 on c4.id = case k.class when 1 then  k.idc_nakup else k.idc_predaj end
                                                    left join hdo.cm2_customer agg_gr on agg_gr.id = o.id_posk_mer_dat
                                           WHERE
                                               oadeal.ID_ATTR      = 2
                                             AND o.nazov = ?
                                           ORDER BY 2 DESC)
                            WHERE ROWNUM <= 1
                            """
            );

            preparedStatement.setString(1, podIdentifier);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Timestamp dateFrom = resultSet.getTimestamp("date_from", Calendar.getInstance());
                Timestamp dateTo = resultSet.getTimestamp("date_to", Calendar.getInstance());
                String owner = resultSet.getString("owner");
                Long pdtOwner = resultSet.getLong("PDT_owner");
                String eanOrEIC = resultSet.getString("EAN/EIC");
                String dealId = resultSet.getString("deal_id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                String type = resultSet.getString("type");
                String voltLevel1 = resultSet.getString("volt_level_1");
                String measurementType = resultSet.getString("measurement_type");
                String sourceType = resultSet.getString("source_type");
                String distributionNetwork = resultSet.getString("distribution_network");
                String network = resultSet.getString("network");
                String supplier = resultSet.getString("supplier");
                String lpRegion1 = resultSet.getString("LP_region1");
                String coordinatorOfBalanceGroup = resultSet.getString("coordinator_of_balance_group");
                String measurementSystem = resultSet.getString("measurement_system");
                String ancillaryServicesProvider = resultSet.getString("ancillary_services_provider");
                String meteringDataProvider = resultSet.getString("metering_data_provider");

                AdditionalInformationForPointOfDeliveries additionalInformation = new AdditionalInformationForPointOfDeliveries(
                        dateFrom.toLocalDateTime(),
                        dateTo.toLocalDateTime(),
                        owner,
                        pdtOwner,
                        eanOrEIC,
                        dealId,
                        name,
                        description,
                        type,
                        voltLevel1,
                        measurementType,
                        sourceType,
                        distributionNetwork,
                        network,
                        supplier,
                        lpRegion1,
                        "",
                        coordinatorOfBalanceGroup,
                        measurementSystem,
                        ancillaryServicesProvider,
                        meteringDataProvider
                );

                if (resultSet.next()) {
                    throw new XEnergieException("More then one additional information found for POD identifier: [%s]".formatted(podIdentifier));
                }

                return additionalInformation;
            } else {
                throw new XEnergieException("Additional information for POD identifier: [%s] not found".formatted(podIdentifier));
            }
        }
    }

    public List<BalancingSystemsProducts> retrieveBalancingSystemProducts() throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            SELECT distinct
                            p.nazov product_name
                            from hdo.cm2_produkt p
                            join hdo.time_series_v t on t.id_directory = hdo.profile_manager.GET_DIR_ID(p.adresar)
                            where p.typ = 16
                            and t.type = 1
                            and t.expression = '0'
                            """);

            ResultSet resultSet = preparedStatement.executeQuery();

            List<BalancingSystemsProducts> balancingSystemsProducts = new ArrayList<>();

            while (resultSet.next()) {
                balancingSystemsProducts.add(new BalancingSystemsProducts(
                        resultSet.getString("product_name")
                ));
            }

            return balancingSystemsProducts;
        }
    }

    public List<BalancingSystemsProfiles> retrieveBalancingSystemProfiles() throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            SELECT distinct
                            t.name profile_name
                            from hdo.cm2_produkt p
                            join hdo.time_series_v t on t.id_directory = hdo.profile_manager.GET_DIR_ID(p.adresar)
                            where p.typ = 16
                            and t.type = 1
                            and t.expression = '0'
                            """);

            ResultSet resultSet = preparedStatement.executeQuery();

            List<BalancingSystemsProfiles> balancingSystemsProfiles = new ArrayList<>();

            while (resultSet.next()) {
                balancingSystemsProfiles.add(new BalancingSystemsProfiles(
                        resultSet.getString("profile_name")
                ));
            }

            return balancingSystemsProfiles;
        }
    }

    public XEnergieRepositoryCreateCustomerResponse createCustomer(String customerName, String customerNumber, String customerIdentifier) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            CallableStatement callableStatement = connection.prepareCall(
                    """
                            {? = call hdo.importbg.importcustomer(?, ?, ?, ?, ?, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)}
                            """
            );

            callableStatement.registerOutParameter(1, Types.FLOAT);
            callableStatement.setLong(2, 1001L); // group flag
            callableStatement.setString(3, customerName);
            callableStatement.setString(4, customerName);
            callableStatement.setString(5, customerNumber);
            callableStatement.setString(6, customerIdentifier);

            callableStatement.execute();

            float result = callableStatement.getFloat(1);

            callableStatement.close();

            return XEnergieRepositoryCreateCustomerResponse.fromResult(result);
        }
    }

    public boolean isDealExistsForCustomer(String dealNumber, String customerIdentifier) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            SELECT
                            k.cislo as deal, c.dic as uic
                            FROM hdo.cm2_kontrakt k
                            JOIN hdo.cm2_customer c ON k.idc_nakup = c.id
                            JOIN hdo.cm2_customer c2 ON k.idc_predaj = c2.id
                            WHERE k.cislo = ?
                            AND (
                                c.dic = ?
                                OR c2.dic = ?
                                )
                            """
            );

            preparedStatement.setString(1, dealNumber);
            preparedStatement.setString(2, customerIdentifier);
            preparedStatement.setString(3, customerIdentifier);

            ResultSet resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        }
    }

    public XEnergieDealInformation retrieveDealInformation(String dealNumber) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT cislo                                  as deal,
                           TRUNC(hdo.time2date(dat_od, 1)) AS date_from,
                           TRUNC(hdo.time2date(dat_do, 1)) AS date_to
                    FROM hdo.cm2_kontrakt
                    WHERE cislo = ?
                    """);

            preparedStatement.setLong(1, Long.parseLong(dealNumber));

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new XEnergieException("Deal information not found for provided deal number: [%s]".formatted(dealNumber));
            }

            return new XEnergieDealInformation(
                    resultSet.getString(1),
                    resultSet.getDate(2).toLocalDate(),
                    resultSet.getDate(3).toLocalDate()
            );
        }
    }

    /**
     * Delete Empty Splits Before Date
     *
     * @param pointOfDeliveryIdentifier - Point Of Delivery Identifier
     * @param beforeDate                - Date that splits must be deleted before
     * @throws Exception - if exception handled while communicating with database
     */
    public void deleteEmptySplitsBeforeDate(String pointOfDeliveryIdentifier, LocalDate beforeDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    DELETE
                      FROM hdo.opm
                      WHERE id_opm IN (SELECT *
                                       FROM (SELECT o.id_opm
                                             FROM hdo.opm o
                                                      LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                      LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                             WHERE oa.id_attr = 2
                                               AND o.id_subjekt_zuct = '1173'
                                               AND ean = ?
                                               AND hdo.time2date(o.cas_do, 1) <= to_date(?, 'yyyy-MM-dd')
                                               AND c.cislo IS NULL
                                               AND hdo.time2date(o.cas_od, 1) >= (SELECT *
                                                                                  FROM (SELECT hdo.time2date(o.cas_do, 1) AS date_to
                                                                                        FROM hdo.opm o
                                                                                                 LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                                                                 LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                                                                        WHERE oa.id_attr = 2
                                                                                          AND o.id_subjekt_zuct = '1173'
                                                                                          AND ean = ?
                                                                                          AND hdo.time2date(o.cas_do, 1) <= to_date(?, 'yyyy-MM-dd')
                                                                                          AND c.cislo IS NOT NULL
                                                                                        ORDER BY 1 ASC)
                                                                                  WHERE ROWNUM = 1)
                                             ORDER BY 1)
                                       UNION ALL
                                       SELECT o.id_opm
                                       FROM hdo.opm o
                                                LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                       WHERE oa.id_attr = 2
                                         AND o.id_subjekt_zuct = '1173'
                                         AND ean = ?
                                         AND (hdo.time2date(o.cas_do, 1) >= to_date(?, 'yyyy-MM-dd') AND
                                              hdo.time2date(o.cas_od, 1) < to_date(?, 'yyyy-MM-dd')
                                           OR hdo.time2date(o.cas_do, 1) > to_date(?, 'yyyy-MM-dd') AND
                                              hdo.time2date(o.cas_od, 1) <= to_date(?, 'yyyy-MM-dd'))
                                         AND c.cislo IS NULL)
                    """);

            preparedStatement.setString(1, pointOfDeliveryIdentifier);
            preparedStatement.setString(2, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(3, pointOfDeliveryIdentifier);
            preparedStatement.setString(4, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(5, pointOfDeliveryIdentifier);
            preparedStatement.setString(6, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(7, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(8, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(9, beforeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            preparedStatement.executeUpdate();
        }
    }

    /**
     * Delete Empty Splits After Date
     *
     * @param pointOfDeliveryIdentifier - Point Of Delivery Identifier
     * @param afterDate                 - Date that splits must be deleted after
     * @throws Exception - if exception handled while communicating with database
     */
    public void deleteEmptySplitsAfterDate(String pointOfDeliveryIdentifier, LocalDate afterDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    DELETE
                       FROM hdo.opm
                       WHERE id_opm IN (SELECT *
                                        FROM (SELECT o.id_opm
                                              FROM hdo.opm o
                                                       LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                       LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                              WHERE oa.id_attr = 2
                                                AND o.id_subjekt_zuct = '1173'
                                                AND ean = ?
                                                AND hdo.time2date(o.cas_od, 1) >= to_date(?, 'yyyy-MM-dd')
                                                AND c.cislo IS NULL
                                                AND hdo.time2date(o.cas_do, 1) <= (SELECT *
                                                                                   FROM (SELECT hdo.time2date(o.cas_od, 1) AS date_from
                                                                                         FROM hdo.opm o
                                                                                                  LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                                                                  LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                                                                         WHERE oa.id_attr = 2
                                                                                           AND o.id_subjekt_zuct = '1173'
                                                                                           AND ean = ?
                                                                                           AND hdo.time2date(o.cas_od, 1) >= to_date(?, 'yyyy-MM-dd')
                                                                                           AND c.cislo IS NOT NULL
                                                                                         ORDER BY 1 ASC)
                                                                                   WHERE ROWNUM = 1)
                                              ORDER BY 1)
                                        UNION ALL
                                        SELECT o.id_opm
                                        FROM hdo.opm o
                                                 LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                                                 LEFT JOIN hdo.cm2_kontrakt c ON oa.val_int = c.cislo
                                        WHERE oa.id_attr = 2
                                          AND o.id_subjekt_zuct = '1173'
                                          AND ean = ?
                                          AND (hdo.time2date(o.cas_do, 1) >= to_date(?, 'yyyy-MM-dd') AND
                                               hdo.time2date(o.cas_od, 1) < to_date(?, 'yyyy-MM-dd')
                                            OR hdo.time2date(o.cas_do, 1) > to_date(?, 'yyyy-MM-dd') AND
                                               hdo.time2date(o.cas_od, 1) <= to_date(?, 'yyyy-MM-dd'))
                                          AND c.cislo IS NULL)
                    """);

            preparedStatement.setString(1, pointOfDeliveryIdentifier);
            preparedStatement.setString(2, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(3, pointOfDeliveryIdentifier);
            preparedStatement.setString(4, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(5, pointOfDeliveryIdentifier);
            preparedStatement.setString(6, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(7, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(8, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(9, afterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            preparedStatement.executeUpdate();
        }
    }

    public XEnergieSplitInfo getSplitInfo(Long splitId) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT DISTINCT o.id_opm                                                 as split_id,
                                    hdo.time2date(o.cas_od, 1)                               as date_from,
                                    hdo.time2date(o.cas_do, 1) -1                            as date_to,
                                    o.ean                                                    as point_of_delivery,
                                    oa.val_int                                               as deal,
                                    (CASE WHEN o.id_subjekt_zuct = '1173' THEN 1 ELSE 0 END) as delete_available
                    FROM hdo.opm o
                             LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                    WHERE oa.id_attr = 2
                      and o.id_opm = ?
                    """);

            preparedStatement.setLong(1, splitId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                throw new XEnergieException("Split info not found with id: [%s]".formatted(splitId));
            }

            Date dateFrom = resultSet.getDate("date_from");
            Date dateTo = resultSet.getDate("date_to");
            String pointOfDelivery = resultSet.getString("point_of_delivery");
            boolean deleteAvailable = resultSet.getBoolean("delete_available");
            long deal = resultSet.getLong("deal");

            XEnergieSplitInfo splitInfo = XEnergieSplitInfo
                    .builder()
                    .id(splitId)
                    .dateFrom(dateFrom.toLocalDate())
                    .dateTo(dateTo.toLocalDate())
                    .pointOfDeliveryIdentifier(pointOfDelivery)
                    .deleteAvailable(deleteAvailable)
                    .dealId(deal)
                    .build();

            if (resultSet.next()) {
                throw new XEnergieException("Multiple split info found with id: [%s]".formatted(splitId));
            }

            return splitInfo;
        }
    }

    public void updateSplitStartDate(Long splitId, LocalDate startDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE hdo.opm
                    SET cas_od = hdo.date2time(to_date(?, 'yyyy-MM-dd'), 1)
                    WHERE id_opm = ?
                    """);

            preparedStatement.setString(1, startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setLong(2, splitId);

            preparedStatement.executeUpdate();
        }
    }

    public void updateSplitEndDate(Long splitId, LocalDate endDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE hdo.opm
                    SET cas_do = hdo.date2time(to_date(?, 'yyyy-MM-dd'), 1)
                    WHERE id_opm = ?
                    """);

            preparedStatement.setString(1, endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setLong(2, splitId);

            preparedStatement.executeUpdate();
        }
    }

    public void updateSplitDates(Long splitId, LocalDate startDate, LocalDate endDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE hdo.opm
                    SET cas_do = hdo.date2time(to_date(?, 'yyyy-MM-dd'), 1), cas_od = hdo.date2time(to_date(?, 'yyyy-MM-dd'), 1)
                    WHERE id_opm = ?
                    """);

            preparedStatement.setString(1, startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(2, endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setLong(3, splitId);

            preparedStatement.executeUpdate();
        }
    }

    public void updateSplitDealNumber(Long splitId, Long dealId) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE hdo.opm_attr oa
                    SET val_int = ?
                    WHERE ID_attr = 2 AND id_opm = ?
                    """);

            preparedStatement.setLong(1, dealId);
            preparedStatement.setLong(2, splitId);

            preparedStatement.executeUpdate();
        }
    }

    public void deleteSplit(Long splitId) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    DELETE FROM hdo.opm WHERE id_opm = ?
                    """);

            preparedStatement.setLong(1, splitId);

            PreparedStatement preparedStatementForAttributes = connection.prepareStatement("""
                    DELETE FROM hdo.opm_attr WHERE id_opm = ?
                    """);

            preparedStatementForAttributes.setLong(1, splitId);

            preparedStatement.executeUpdate();
            preparedStatementForAttributes.executeUpdate();
        }
    }

    public List<XEnergieEmptySpaceInfo> getEmptySpaces(String pointOfDeliveryIdentifier) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            List<XEnergieEmptySpaceInfo> emptySpaceInfos = new ArrayList<>();

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    WITH DateGaps AS (SELECT hdo.opm.cas_do                                      AS current_end_date,
                                             LEAD(hdo.opm.cas_od) OVER (ORDER BY hdo.opm.cas_do) AS next_start_date
                                      FROM hdo.opm
                                      WHERE ean = ?)
                    SELECT hdo.time2date(current_end_date, 1) AS start_date,
                           hdo.time2date(next_start_date, 1) -1  AS end_date
                    FROM DateGaps
                    WHERE DateGaps.current_end_date + 1 < DateGaps.next_start_date
                    UNION ALL
                    SELECT start_date, end_date
                    FROM (SELECT MAX(hdo.time2date(hdo.opm.cas_do, 1)) AS start_date,
                                 TO_DATE('2030-12-31', 'YYYY-MM-DD')   AS end_date
                          FROM hdo.opm
                          WHERE ean = ?)
                    WHERE start_date < end_date
                    """);

            preparedStatement.setString(1, pointOfDeliveryIdentifier);
            preparedStatement.setString(2, pointOfDeliveryIdentifier);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Date startDate = resultSet.getDate("start_date");
                Date endDate = resultSet.getDate("end_date");

                emptySpaceInfos.add(
                        new XEnergieEmptySpaceInfo(
                                startDate.toLocalDate(),
                                endDate.toLocalDate()
                        )
                );
            }

            return emptySpaceInfos;
        }
    }

    public Long getSplitIdByDealNumberIdentifierAndActivationDates(Long dealNumber, String pointOfDeliveryIdentifier, LocalDate startDate, LocalDate endDate) throws Exception {
        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT o.id_opm as splitId
                    FROM hdo.opm o
                             LEFT JOIN hdo.opm_attr oa ON o.id_opm = oa.id_opm
                    WHERE oa.id_attr = 2
                      AND o.cas_od = hdo.date2time(to_date(?, 'yyyy-MM-dd'), 1)
                      AND TRUNC(hdo.time2date(o.CAS_DO - 7200, 1)) = to_date(?, 'yyyy-MM-dd')
                      AND oa.val_int = ?
                      AND ean = ?
                    """);

            preparedStatement.setString(1, startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setString(2, endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            preparedStatement.setLong(3, dealNumber);
            preparedStatement.setString(4, pointOfDeliveryIdentifier);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Long splitId = resultSet.getLong("splitId");

                if (resultSet.next()) {
                    throw new XEnergieException("More then one split found");
                }

                return Optional.of(splitId).orElseThrow(() -> new XEnergieException("Exception handled while trying to get splitId, split not found;"));
            } else {
                throw new XEnergieException("Exception handled while trying to get splitId, split not found;");
            }
        }
    }

    public List<AdditionalInformationForPointOfDeliveries> splitListing(String pointOfDeliveryIdentifier) throws Exception {
        List<AdditionalInformationForPointOfDeliveries> information = new ArrayList<>();

        try (Connection connection = databaseConnectionService.openConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT DISTINCT TRUNC(hdo.time2date(o.CAS_OD, 1))                   AS "date_from",
                                              TRUNC(hdo.time2date(o.CAS_DO - 7200, 1)) AS "date_to",
                                              c.NAME                                   AS "owner",
                                              c.EAN                                    AS "PDT_owner",
                                              o.EAN                                    AS "EAN/EIC",
                                              case
                                                  when (c.dic = c4.dic or k.class = 2) then oadeal.VAL_INT
                                                  else null end                        AS "deal_id",
                                              o.NAZOV                                  AS "name",
                                              o.POPIS                                  AS "description",
                                              o.TYP_OPM                                AS "type",
                                              o.NAP_UROVEN                             AS "volt_level_1",
                                              o.TYP_MER                                AS "measurement_type",
                                              o.TYP_ZDROJ                              AS "source_type",
                                              '000' || to_char(o.ID_SIET)              AS "distribution_network",
                                              OPM_SIET.NAZOV                           AS "network",
                                              c2.NAME                                  AS "supplier",
                                              o.ID_OBLAST_TDD                          AS "LP_region1",
                                              OPM_TDD_OBLAST.POPIS                     AS "LP_region2",
                                              c3.NAME                                  AS "coordinator_of_balance_group",
                                              oamsr.VAL_STR                            AS "measurement_system",
                                              asp_cust.ean || ' ' || asp_cust.name     as "ancillary_services_provider",
                                              agg_gr.ean                               as "metering_data_provider"
                    FROM hdo.opm o
                             LEFT JOIN hdo.CM2_CUSTOMER c
                                       ON o.ID_OWNER = c.ID
                             LEFT JOIN hdo.OPM_ATTR oadeal
                                       ON o.ID_OPM = oadeal.ID_OPM
                             INNER JOIN hdo.OPM_SIET
                                        ON o.ID_SIET = OPM_SIET.ID_SIET
                             LEFT JOIN hdo.CM2_CUSTOMER c2
                                       ON o.ID_DOD = c2.ID
                             LEFT JOIN hdo.OPM_TDD_OBLAST
                                       ON o.ID_OBLAST_TDD = OPM_TDD_OBLAST.ID
                             LEFT JOIN hdo.OPM_ATTR oamsr
                                       ON o.ID_OPM = oamsr.ID_OPM
                             LEFT JOIN hdo.CM2_CUSTOMER c3
                                       ON o.ID_SUBJEKT_ZUCT = c3.ID
                             LEFT JOIN hdo.cm2_customer asp_cust on asp_cust.id = o.id_posk_pps
                             left join hdo.cm2_kontrakt k on k.cislo = oadeal.VAL_INT
                             left join hdo.CM2_CUSTOMER c4 on c4.id = case k.class when 1 then k.idc_nakup else k.idc_predaj end
                             left join hdo.cm2_customer agg_gr on agg_gr.id = o.id_posk_mer_dat
                    WHERE o.ID_SUBJEKT_ZUCT = 1173
                      AND oadeal.ID_ATTR = 2
                      AND o.nazov = ?
                    """);

            preparedStatement.setString(1, pointOfDeliveryIdentifier);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Timestamp dateFrom = resultSet.getTimestamp("date_from", Calendar.getInstance());
                Timestamp dateTo = resultSet.getTimestamp("date_to", Calendar.getInstance());
                String owner = resultSet.getString("owner");
                Long pdtOwner = resultSet.getLong("PDT_owner");
                String eanOrEIC = resultSet.getString("EAN/EIC");
                String dealId = resultSet.getString("deal_id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                String type = resultSet.getString("type");
                String voltLevel1 = resultSet.getString("volt_level_1");
                String measurementType = resultSet.getString("measurement_type");
                String sourceType = resultSet.getString("source_type");
                String distributionNetwork = resultSet.getString("distribution_network");
                String network = resultSet.getString("network");
                String supplier = resultSet.getString("supplier");
                String lpRegion1 = resultSet.getString("LP_region1");
                String lpRegion2 = resultSet.getString("LP_region2");
                String coordinatorOfBalanceGroup = resultSet.getString("coordinator_of_balance_group");
                String measurementSystem = resultSet.getString("measurement_system");
                String ancillaryServicesProvider = resultSet.getString("ancillary_services_provider");
                String meteringDataProvider = resultSet.getString("metering_data_provider");

                AdditionalInformationForPointOfDeliveries additionalInformation = new AdditionalInformationForPointOfDeliveries(
                        dateFrom.toLocalDateTime(),
                        dateTo.toLocalDateTime(),
                        owner,
                        pdtOwner,
                        eanOrEIC,
                        dealId,
                        name,
                        description,
                        type,
                        voltLevel1,
                        measurementType,
                        sourceType,
                        distributionNetwork,
                        network,
                        supplier,
                        lpRegion1,
                        lpRegion2,
                        coordinatorOfBalanceGroup,
                        measurementSystem,
                        ancillaryServicesProvider,
                        meteringDataProvider
                );

                information.add(additionalInformation);
            }
        }
        return information;
    }
}

package bg.energo.phoenix.util.epb;

import org.springframework.util.CollectionUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public class EPBDatabaseFunctionUtils {

    /**
     * Sets a {@link Long} parameter in a {@link CallableStatement}, handling null values.
     *
     * @param cst   the {@link CallableStatement} to set the parameter on
     * @param value the {@link Long} value to set, or {@code null} to set SQL NULL
     * @param index the parameter index in the {@link CallableStatement}
     * @throws SQLException if a database access error occurs or this method is called on a closed {@link CallableStatement}
     */
    public static void nullSafeSetLong(CallableStatement cst, Long value, int index) throws SQLException {
        if (Objects.isNull(value)) {
            cst.setNull(index, Types.BIGINT);
        } else {
            cst.setLong(index, value);
        }
    }

    /**
     * Sets a {@link String} parameter in a {@link CallableStatement}, handling null values.
     *
     * @param cst   the {@link CallableStatement} to set the parameter on
     * @param value the {@link String} value to set, or {@code null} to set SQL NULL
     * @param index the parameter index in the {@link CallableStatement}
     * @throws SQLException if a database access error occurs or this method is called on a closed {@link CallableStatement}
     */
    public static void nullSafeSetString(CallableStatement cst, String value, int index) throws SQLException {
        if (Objects.isNull(value)) {
            cst.setNull(index, Types.VARCHAR);
        } else {
            cst.setString(index, value);
        }
    }

    /**
     * Sets a {@link Boolean} parameter in a {@link CallableStatement}, handling null values.
     *
     * @param cst   the {@link CallableStatement} to set the parameter on
     * @param value the {@link Boolean} value to set, or {@code null} to set SQL NULL
     * @param index the parameter index in the {@link CallableStatement}
     * @throws SQLException if a database access error occurs or this method is called on a closed {@link CallableStatement}
     */
    public static void nullSafeSetBoolean(CallableStatement cst, Boolean value, int index) throws SQLException {
        if (Objects.isNull(value)) {
            cst.setNull(index, Types.BOOLEAN);
        } else {
            cst.setBoolean(index, value);
        }
    }

    /**
     * Sets a list of Long values into the {@link CallableStatement} in a null-safe way.
     * If the provided {@code values} set is null or empty, it will set the corresponding parameter
     * to {@code NULL} in the database. If the set contains values, it will convert the set into
     * an array and set it to the callable statement.
     *
     * @param cst    The {@link CallableStatement} to which the parameter should be set.
     * @param values The set of Long values to set, may be null or empty.
     * @param index  The index of the parameter to be set.
     * @throws SQLException If an SQL error occurs while setting the parameter.
     */
    public static void nullSafeSetLongList(CallableStatement cst, Set<Long> values, int index) throws SQLException {
        if (CollectionUtils.isEmpty(values)) {
            cst.setNull(index, Types.ARRAY);
        } else {
            Array array = cst.getConnection().createArrayOf("BIGINT", values.toArray());
            cst.setArray(index, array);
        }
    }


    /**
     * Sets a {@link LocalDate} value into the {@link CallableStatement} in a null-safe way.
     * If the provided {@code value} is null, it will set the corresponding parameter to
     * {@code NULL} in the database. If the value is non-null, it will be converted to
     * a {@link Date} and set to the callable statement.
     *
     * @param cst   The {@link CallableStatement} to which the parameter should be set.
     * @param value The LocalDate value to set, may be null.
     * @param index The index of the parameter to be set.
     * @throws SQLException If an SQL error occurs while setting the parameter.
     */
    public static void nullSafeSetLocalDate(CallableStatement cst, LocalDate value, int index) throws SQLException {
        if (value == null) {
            cst.setNull(index, Types.DATE);
        } else {
            cst.setDate(index, Date.valueOf(value));
        }
    }


}

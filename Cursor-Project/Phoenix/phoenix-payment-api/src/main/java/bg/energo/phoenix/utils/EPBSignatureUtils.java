package bg.energo.phoenix.utils;

import bg.energo.phoenix.model.request.ConfirmPayRequest;
import bg.energo.phoenix.model.request.InitPayRequest;
import bg.energo.phoenix.cashterminal.model.request.CashTerminalConfirmPayRequest;
import bg.energo.phoenix.cashterminal.model.request.CashTerminalInitPayRequest;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosConfirmPayRequest;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosInitPayRequest;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Objects;

public class EPBSignatureUtils {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * Calculates the HMAC (Hashed Message Authentication Code) for the given data using the provided key.
     *
     * @param key  The secret key used for HMAC calculation.
     * @param data The data to be hashed.
     * @return The hexadecimal string representation of the HMAC.
     * @throws NoSuchAlgorithmException If the HMAC algorithm is not available.
     * @throws InvalidKeyException      If the provided key is invalid for HMAC.
     */
    public static String calculateHMAC(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, IllegalArgumentException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty.");
        }
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty.");
        }

        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
        mac.init(secretKeySpec);

        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return EPBSignatureUtils.byteToHex(rawHmac);
    }

    /**
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param bytes The byte array to convert.
     * @return A hexadecimal string representing the byte array.
     * @throws IllegalArgumentException if the input byte array is null.
     */
    public static String byteToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Input byte array cannot be null.");
        }

        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    /**
     * Checks if the signature of an initialization payment request is invalid.
     * The method calculates an HMAC (Hash-based Message Authentication Code) using the provided
     * {@code key} and compares it with the provided {@code requestCheckSum}.
     * If the calculated checksum is empty or does not match the provided checksum,
     * or if there are any exceptions during the process, the signature is considered invalid.
     *
     * @param request         The initialization payment request whose fields will be used to calculate the checksum.
     * @param requestCheckSum The checksum received in the request, used to compare with the calculated checksum.
     * @param key             The secret key used to calculate the HMAC.
     * @return {@code true} if the signature is invalid, otherwise {@code false}.
     */
    public static boolean isInvalidInitSignature(InitPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatInitRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Checks if the signature of a payment confirmation request is invalid.
     * The method calculates an HMAC (Hash-based Message Authentication Code) using the provided
     * {@code key} and compares it with the provided {@code requestCheckSum}.
     * If the calculated checksum is empty or does not match the provided checksum,
     * or if there are any exceptions during the process, the signature is considered invalid.
     *
     * @param request         The payment confirmation request whose fields will be used to calculate the checksum.
     * @param requestCheckSum The checksum received in the request, used to compare with the calculated checksum.
     * @param key             The secret key used to calculate the HMAC.
     * @return {@code true} if the signature is invalid, otherwise {@code false}.
     */
    public static boolean isInvalidPaySignature(ConfirmPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatConfirmPayRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return true;
        }
    }

    /**
     * Formats the fields of an {@link InitPayRequest} object into a single string.
     * Each non-null field is included in the format `KEYVALUE`, with each key-value pair
     * on a new line.
     * The following fields are included in the output:
     * <ul>
     *     <li>IDN</li>
     *     <li>MERCHANTID</li>
     *     <li>TID</li>
     *     <li>TYPE</li>
     * </ul>
     * If a field is null, it is omitted from the result.
     *
     * @param initPayRequest The {@link InitPayRequest} object containing the fields to be formatted.
     *                       Should not be {@code null}.
     * @return A string with each key-value pair formatted as `KEYVALUE`, separated by new lines.
     * If no fields are non-null, returns an empty string.
     */
    public static String formatInitRequestFields(InitPayRequest initPayRequest) {
        StringBuilder sb = new StringBuilder();

        if (Objects.nonNull(initPayRequest.getIDN())) {
            sb.append("IDN").append(initPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(initPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTID())) {
            sb.append("TID").append(initPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTYPE())) {
            sb.append("TYPE").append(initPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

    /**
     * Formats the fields of a {@link ConfirmPayRequest} object into a single string.
     * Each non-null field is included in the format `KEYVALUE`, with each key-value pair
     * on a new line.
     * The following fields are included in the output:
     * <ul>
     *     <li>IDN</li>
     *     <li>MERCHANTID</li>
     *     <li>TID</li>
     *     <li>DATE</li>
     *     <li>TYPE</li>
     *     <li>TOTAL</li>
     * </ul>
     * If a field is null, it is omitted from the result.
     *
     * @param confirmPayRequest The {@link ConfirmPayRequest} object containing the fields to be formatted.
     *                          This parameter must not be {@code null}.
     * @return A string where each key-value pair is formatted as `KEYVALUE` and separated by new lines.
     * If no fields are non-null, the method returns an empty string.
     */
    public static String formatConfirmPayRequestFields(ConfirmPayRequest confirmPayRequest) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(confirmPayRequest.getDATE())) {
            sb.append("DATE").append(confirmPayRequest.getDATE()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getIDN())) {
            sb.append("IDN").append(confirmPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getINVOICES())) {
            sb.append("INVOICES").append(confirmPayRequest.getINVOICES()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(confirmPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTID())) {
            sb.append("TID").append(confirmPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTOTAL())) {
            sb.append("TOTAL").append(confirmPayRequest.getTOTAL()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTYPE())) {
            sb.append("TYPE").append(confirmPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

    public static boolean isInvalidCashTerminalInitSignature(CashTerminalInitPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatCashTerminalInitRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return true;
        }
    }

    public static boolean isInvalidCashTerminalPaySignature(CashTerminalConfirmPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatCashTerminalConfirmPayRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return true;
        }
    }

    public static String formatCashTerminalInitRequestFields(CashTerminalInitPayRequest initPayRequest) {
        StringBuilder sb = new StringBuilder();

        if (Objects.nonNull(initPayRequest.getIDN())) {
            sb.append("IDN").append(initPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(initPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTID())) {
            sb.append("TID").append(initPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTYPE())) {
            sb.append("TYPE").append(initPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

    public static String formatCashTerminalConfirmPayRequestFields(CashTerminalConfirmPayRequest confirmPayRequest) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(confirmPayRequest.getDATE())) {
            sb.append("DATE").append(confirmPayRequest.getDATE()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getIDN())) {
            sb.append("IDN").append(confirmPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getINVOICES())) {
            sb.append("INVOICES").append(confirmPayRequest.getINVOICES()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(confirmPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTID())) {
            sb.append("TID").append(confirmPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTOTAL())) {
            sb.append("TOTAL").append(confirmPayRequest.getTOTAL()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTYPE())) {
            sb.append("TYPE").append(confirmPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

    /**
     * Checks if the signature of a VirtualPos initialization payment request is invalid.
     * The method calculates an HMAC (Hash-based Message Authentication Code) using the provided
     * {@code key} and compares it with the provided {@code requestCheckSum}.
     * If the calculated checksum is empty or does not match the provided checksum,
     * or if there are any exceptions during the process, the signature is considered invalid.
     *
     * @param request The VirtualPos initialization payment request whose fields will be used to calculate the checksum.
     * @param requestCheckSum The checksum received in the request, used to compare with the calculated checksum.
     * @param key The secret key used to calculate the HMAC.
     * @return {@code true} if the signature is invalid, otherwise {@code false}.
     */
    public static boolean isInvalidVirtualPosInitSignature(VirtualPosInitPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatVirtualPosInitRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Checks if the signature of a VirtualPos payment confirmation request is invalid.
     * The method calculates an HMAC (Hash-based Message Authentication Code) using the provided
     * {@code key} and compares it with the provided {@code requestCheckSum}.
     * If the calculated checksum is empty or does not match the provided checksum,
     * or if there are any exceptions during the process, the signature is considered invalid.
     *
     * @param request The VirtualPos payment confirmation request whose fields will be used to calculate the checksum.
     * @param requestCheckSum The checksum received in the request, used to compare with the calculated checksum.
     * @param key The secret key used to calculate the HMAC.
     * @return {@code true} if the signature is invalid, otherwise {@code false}.
     */
    public static boolean isInvalidVirtualPosPaySignature(VirtualPosConfirmPayRequest request, String requestCheckSum, String key) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(key, EPBSignatureUtils.formatVirtualPosConfirmPayRequestFields(request));
            return StringUtils.isEmpty(checkSum) || !Objects.equals(requestCheckSum, checkSum);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Formats the fields of a {@link VirtualPosInitPayRequest} object into a single string.
     * Each non-null field is included in the format `KEYVALUE`, with each key-value pair
     * on a new line.
     * The following fields are included in the output:
     * <ul>
     *     <li>IDN</li>
     *     <li>MERCHANTID</li>
     *     <li>TID</li>
     *     <li>TYPE</li>
     * </ul>
     * If a field is null, it is omitted from the result.
     *
     * @param initPayRequest The {@link VirtualPosInitPayRequest} object containing the fields to be formatted.
     *                       Should not be {@code null}.
     * @return A string with each key-value pair formatted as `KEYVALUE`, separated by new lines.
     *         If no fields are non-null, returns an empty string.
     */
    public static String formatVirtualPosInitRequestFields(VirtualPosInitPayRequest initPayRequest) {
        StringBuilder sb = new StringBuilder();

        if (Objects.nonNull(initPayRequest.getIDN())) {
            sb.append("IDN").append(initPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(initPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTID())) {
            sb.append("TID").append(initPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(initPayRequest.getTYPE())) {
            sb.append("TYPE").append(initPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

    /**
     * Formats the fields of a {@link VirtualPosConfirmPayRequest} object into a single string.
     * Each non-null field is included in the format `KEYVALUE`, with each key-value pair
     * on a new line.
     * The following fields are included in the output:
     * <ul>
     *     <li>IDN</li>
     *     <li>MERCHANTID</li>
     *     <li>TID</li>
     *     <li>DATE</li>
     *     <li>TYPE</li>
     *     <li>TOTAL</li>
     * </ul>
     * If a field is null, it is omitted from the result.
     *
     * @param confirmPayRequest The {@link VirtualPosConfirmPayRequest} object containing the fields to be formatted.
     *                          This parameter must not be {@code null}.
     * @return A string where each key-value pair is formatted as `KEYVALUE` and separated by new lines.
     *         If no fields are non-null, the method returns an empty string.
     */
    public static String formatVirtualPosConfirmPayRequestFields(VirtualPosConfirmPayRequest confirmPayRequest) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(confirmPayRequest.getDATE())) {
            sb.append("DATE").append(confirmPayRequest.getDATE()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getIDN())) {
            sb.append("IDN").append(confirmPayRequest.getIDN()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getINVOICES())) {
            sb.append("INVOICES").append(confirmPayRequest.getINVOICES()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getMERCHANTID())) {
            sb.append("MERCHANTID").append(confirmPayRequest.getMERCHANTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTID())) {
            sb.append("TID").append(confirmPayRequest.getTID()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTOTAL())) {
            sb.append("TOTAL").append(confirmPayRequest.getTOTAL()).append('\n');
        }
        if (Objects.nonNull(confirmPayRequest.getTYPE())) {
            sb.append("TYPE").append(confirmPayRequest.getTYPE()).append('\n');
        }

        return sb.toString();
    }

}

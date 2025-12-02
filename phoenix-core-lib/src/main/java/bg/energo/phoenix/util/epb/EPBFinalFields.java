package bg.energo.phoenix.util.epb;

public class EPBFinalFields {

    /**
     * Abbreviation for denoting sensitive data that should not be displayed.
     */
    public static final String GDPR = "GDPR";


    /**
     * This flag is used to indicate that the validation message should be removed from the response.
     * It is used in {@link bg.energo.phoenix.service.customer.CustomerService} mainly to filter out the
     * validation messages which still indicate that there was a validation error, but the error is not relevant for the user.
     */
    public static final String VALIDATION_MESSAGE_REMOVE_INDICATOR = "REMOVE_VALIDATION_MESSAGE";


    /**
     * Prefix that should be used for the contract number of a product contract.
     */
    public static final String CONTRACT_NUMBER_PREFIX = "EPES";


    /**
     * Product Contract POD prefix
     */
    public static final String POD_PREFIX = "32Z";


    /**
     * Prefix that should be used for identifying Risk List API decision in error messages.
     */
    public static final String RISK_LIST_DECISION_INDICATOR = "RISK_LIST_DECISION";


    /**
     * Prefix that should be used for identifying Risk List API recommendations in error messages.
     */
    public static final String RISK_LIST_RECOMMENDATIONS_INDICATOR = "RISK_LIST_RECOMMENDATIONS";


    /**
     * Prefix that should be used for identifying warning message in error messages.
     */
    public static final String WARNING_MESSAGE_INDICATOR = "WARNING_MESSAGE";


    /**
     * Delimiter that should be used for separating multiple values in a single string in system activities.
     */
    public static final String SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER = "\\|";


    /**
     * Length of the unique identifier prefix in the file names used across the system including the "underscore" sign following the UUID ("UUID_").
     */
    public static final Integer UUID_PREFIX_LENGTH_IN_FILE_NAME = 37;


    /**
     * Prefix that should be used for the order number of a service order.
     */
    public static final String ORDER_NUMBER_PREFIX = "ORES";


    public static final String IMPORT_TEMPLATE_ID = "GOODS_ORDER_GOODS";


    public static final String MANUAL_INVOICE_DETAILED_TEMPLATE_ID = "MANUAL_INVOICE_DETAILED";

    public static final String MANUAL_INVOICE_STANDARD_TEMPLATE_ID = "MANUAL_INVOICE_STANDARD";


    public static final String INFORMATIONAL_ERROR_MESSAGE_INDICATOR = "INFORMATIONAL_ERROR_MESSAGE";

    public static final String BILLING_RUN_NUMBER_PREFIX = "BILLING";

    public static final String INVOICE_CANCELLATION_TEMPLATE_ID = "INVOICE_CANCELATION";

    public static final Integer MAX_BILLING_RUN_NUMBER_PREFIX = 9999;

    /**
     * Prefix that should be used for the reminder number of a reminder.
     */
    public static final String REMINDER_NUMBER_PREFIX = "REMINDER-";

    /**
     * Prefix that should be used for the number of a Disconnection of Power Supply Request.
     */
    public static final String DISCONNECTION_POWER_SUPPLY_REQUESTS_NUMBER_PREFIX = "Request-";

    /**
     * Prefix that should be used for the number of a Customer Assessment.
     */
    public static final String CUSTOMER_ASSESSMENT_NUMBER_PREFIX = "assessment";

    /**
     * Prefix that should be used for the generated invoice.
     */
    public static final String INVOICE_NUMBER_PREFIX = "INV-";

    public static final String DISCONNECTION_OF_POWER_SUPPLY = "DISCONNECTION_OF_POWER_SUPPLY";

}

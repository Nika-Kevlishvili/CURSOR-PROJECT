package bg.energo.phoenix.util;

import lombok.extern.slf4j.Slf4j;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

@Slf4j
public class RRuleUtil {

    /**
     * Checks if passed rrule is valid, and there are no parameters that needs to be ignored
     *
     * @param rrule rrule string
     * @return {@link RecurrenceRule} RecurrenceRule object if rrule string is valid, null otherwise
     */
    public static RecurrenceRule validRecurrenceRule(String rrule) {
        RecurrenceRule recurrenceRule;
        try {
            recurrenceRule = new RecurrenceRule(rrule, RecurrenceRule.RfcMode.RFC5545_STRICT);
            if (checkRruleValuesToBeIgnored(recurrenceRule)) {
                return recurrenceRule;
            }
        } catch (InvalidRecurrenceRuleException e) {
            log.error("recurrence rule passed - {} - is not valid", rrule);
            return null;
        }
        return recurrenceRule;
    }

    /**
     * Checks if passed date matches rrule
     *
     * @param recurrenceRule RecurrenceRule object
     * @return true if period matches rrule, false otherwise
     */
    public static boolean periodMatchesRRule(RecurrenceRule recurrenceRule, LocalDate billingRunStartDate) {
        //get previous year from billingRunStartDate
        int prevYear = billingRunStartDate.getYear() - 1;

        // Create a ZonedDateTime object for last day of the previous year at midnight in the default time zone
        ZonedDateTime prevYearLastDay = ZonedDateTime.of(prevYear, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime billingTime = ZonedDateTime.of(billingRunStartDate.getYear(), billingRunStartDate.getMonthValue(), billingRunStartDate.getDayOfMonth(), 0, 0, 0, 0, ZoneId.systemDefault());
        // Convert ZonedDateTime to milliseconds since epoch
        long milliseconds = prevYearLastDay.toInstant().toEpochMilli();
        long secondsBillingTime = billingTime.toInstant().toEpochMilli();

        // Create a DateTime object using the default time zone and the milliseconds
        DateTime firstDateTime = new DateTime(TimeZone.getDefault(), milliseconds);

        DateTime billingDateTime = new DateTime(TimeZone.getDefault(), secondsBillingTime);

        //set previous year last day date as first iterator, as iterator doesn't check first occurrence match for rrule
        RecurrenceRuleIterator iterator = recurrenceRule.iterator(firstDateTime);
        while (iterator.hasNext()) {
            DateTime dateTime = iterator.nextDateTime();
            if (dateTime.equals(billingDateTime)) {
                return true;
            }
            if (dateTime.after(billingDateTime)) {
                return false;
            }
        }
        return false;
    }

    private static boolean checkRruleValuesToBeIgnored(RecurrenceRule recurrenceRule) {
        if (List.of(RecurrenceRule.Freq.HOURLY, RecurrenceRule.Freq.MINUTELY, RecurrenceRule.Freq.SECONDLY).contains(recurrenceRule.getFreq())) {
            log.debug("recurrence rule has frequency which must be ignored");
            return false;
        }
        if (recurrenceRule.hasPart(RecurrenceRule.Part.BYHOUR) ||
            recurrenceRule.hasPart(RecurrenceRule.Part.BYSECOND) ||
            recurrenceRule.hasPart(RecurrenceRule.Part.BYMINUTE)) {
            log.debug("recurrence rule has part which must be ignored");
            return false;
        }
        if (recurrenceRule.getInterval() > 1) {
            log.debug("recurrence rule has interval more than 1");
            return false;
        }
        return true;
    }

}

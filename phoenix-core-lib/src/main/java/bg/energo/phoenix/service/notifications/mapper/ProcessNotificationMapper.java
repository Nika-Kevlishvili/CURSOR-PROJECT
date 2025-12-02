package bg.energo.phoenix.service.notifications.mapper;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;

public class ProcessNotificationMapper {
    public static NotificationType mapToNotificationType(ProcessType processType, NotificationState notificationState) {
        switch (processType) {
            case PROCESS_CUSTOMER_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_CUSTOMER_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_CUSTOMER_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_POD_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_POD_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_POD_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_PRODUCT_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_PRODUCT_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_PRODUCT_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_SERVICE_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_SERVICE_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_SERVICE_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_METER_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_METER_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_METER_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PRODUCT_CONTRACT_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PRODUCT_CONTRACT_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PRODUCT_CONTRACT_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_SERVICE_CONTRACT_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_SERVICE_CONTRACT_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_SERVICE_CONTRACT_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_INVOICE_CANCELLATION -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_INVOICE_CANCELLATION_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_INVOICE_CANCELLATION_ERROR;
                    }
                }
            }
            case X_ENERGIE_EXCEPTION_REPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.X_ENERGIE_EXCEPTION_REPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.X_ENERGIE_EXCEPTION_REPORT_ERROR;
                    }
                }
            }
            case PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_PAYMENT_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_PAYMENT_MASS_IMPORT_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_PAYMENT_MASS_IMPORT_ERROR;
                    }
                }
            }
            case PROCESS_REMINDER -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_REMINDER_COMPLETED;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_REMINDER_ERROR;
                    }
                }
            }
            case GOVERNMENT_COMPENSATION_MASS_IMPORT -> {
                switch (notificationState) {
                    case COMPLETION -> {
                        return NotificationType.PROCESS_GOVERNMENT_COMPENSATION_COMPLETE;
                    }
                    case ERROR -> {
                        return NotificationType.PROCESS_GOVERNMENT_COMPENSATION_ERROR;
                    }
                }
            }
        }

        throw new IllegalArgumentsProvidedException("Cannot map process type to notification type");
    }
}

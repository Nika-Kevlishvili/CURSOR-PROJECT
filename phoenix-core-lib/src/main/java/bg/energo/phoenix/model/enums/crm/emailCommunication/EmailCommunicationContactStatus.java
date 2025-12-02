package bg.energo.phoenix.model.enums.crm.emailCommunication;


import bg.energo.mass_comm.models.TaskStatus;

public enum EmailCommunicationContactStatus {
    NEW,
    PENDING,
    IN_PROCESS,
    SUCCESS,
    PARTIAL_SUCCESS,
    ERROR,
    CANCELED,
    NOT_FOUND;

    public static EmailCommunicationContactStatus fromClientStatus(TaskStatus taskStatus) {
        switch (taskStatus) {
            case NEW -> {
                return NEW;
            }
            case PENDING -> {
                return PENDING;
            }
            case IN_PROCESS -> {
                return IN_PROCESS;
            }
            case SUCCESS -> {
                return SUCCESS;
            }
            case PARTIAL_SUCCESS -> {
                return PARTIAL_SUCCESS;
            }
            case ERROR -> {
                return ERROR;
            }
            case CANCELED -> {
                return CANCELED;
            }
            case NOT_FOUND -> {
                return NOT_FOUND;
            }
        }
        return null;
    }
}

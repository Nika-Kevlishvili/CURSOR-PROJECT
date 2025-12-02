package bg.energo.phoenix.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.SystemMessage;
import bg.energo.phoenix.model.request.systemMessage.CreateSystemMessageRequest;
import bg.energo.phoenix.model.request.systemMessage.GetSystemMessageListRequest;
import bg.energo.phoenix.model.request.systemMessage.SystemMessageEditRequest;
import bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponse;
import bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponseListItem;
import bg.energo.phoenix.repository.SystemMessageRepository;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageService {

    private final SystemMessageRepository systemMessageRepository;

    /**
     * <h1>Find SystemMessage with id</h1>
     * takes id of the system message, checks it in database
     * if there is no data in the database throws exception with message: System Message with this id doesn't exists
     * if there is data returns full info of the system message
     *
     * @param id unique system message Object id
     * @return @return {@link SystemMessageResponse} object
     */
    public SystemMessageResponse find(Long id) {
        log.debug("Find system message with id : {}", id);
        SystemMessage systemMessage = systemMessageRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-System Message with ID %s not found;".formatted(id)));
        return new SystemMessageResponse(systemMessage);
    }

    /**
     * <h1>Filter system message</h1>
     * function to return paginated list of the system messages
     * also provides functionality to search system message by name in database
     * list by default is ordered by create date
     *
     * @param request {@link GetSystemMessageListRequest}
     * @return List of paginated @return {@link SystemMessageResponseListItem} objects
     */
    public Page<SystemMessageResponseListItem> filter(GetSystemMessageListRequest request) {
        log.debug("Filter system message: {}", request.toString());
        return systemMessageRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Edits system message record.
     *
     * @param request {@link SystemMessageEditRequest}
     * @return full edited system message object
     */
    @Transactional
    public SystemMessageResponse edit(SystemMessageEditRequest request) {
        log.debug("Edit system message: {}", request.toString());
        trimEditRequestFields(request);

        SystemMessage dbSystemMessage = systemMessageRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-System Message with ID %s not found;".formatted(request.getId())));

        if (!canChangeButtonNames(request, dbSystemMessage)) {
            log.error("id-Button visibility logic cannot be changed;");
            throw new ClientException("id-Button visibility logic cannot be changed;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (!isValidButtonTexts(request, dbSystemMessage)) {
            log.error("id-Button text length should be from 1 to 32;");
            throw new ClientException("id-Button text length should be from 1 to 32", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        dbSystemMessage.setId(request.getId());
        dbSystemMessage.setTitle(request.getTitle().trim());
        dbSystemMessage.setTitleTransliterated(request.getTitleTransliterated().trim());
        dbSystemMessage.setMessageText(request.getMessageText());
        dbSystemMessage.setMessageTextTransliterated(request.getMessageTextTransliterated());
        dbSystemMessage.setOkButtonText(StringUtils.defaultIfBlank(request.getOkButtonText(), null));
        dbSystemMessage.setOkButtonTextTransliterated(StringUtils.defaultIfBlank(request.getOkButtonTextTransliterated(), null));
        dbSystemMessage.setNoButtonText(StringUtils.defaultIfBlank(request.getNoButtonText(), null));
        dbSystemMessage.setNoButtonTextTransliterated(StringUtils.defaultIfBlank(request.getNoButtonTextTransliterated(), null));
        dbSystemMessage.setCancelButtonText(StringUtils.defaultIfBlank(request.getCancelButtonText(), null));
        dbSystemMessage.setCancelButtonTextTransliterated(StringUtils.defaultIfBlank(request.getCancelButtonTextTransliterated(), null));
        dbSystemMessage.setSystemMessageType(request.getSystemMessageType());
        return new SystemMessageResponse(systemMessageRepository.save(dbSystemMessage));
    }

    /**
     * Trims {@link SystemMessageEditRequest} fields.
     *
     * @param request {@link SystemMessageEditRequest}
     */
    private void trimEditRequestFields(SystemMessageEditRequest request) {
        request.setTitle(request.getTitle().trim());
        request.setTitleTransliterated(request.getTitleTransliterated().trim());
        request.setMessageText(request.getMessageText().trim());
        request.setMessageTextTransliterated(request.getMessageTextTransliterated().trim());

        if (isNotEmpty(request.getOkButtonText()) && isNotEmpty(request.getOkButtonTextTransliterated())) {
            request.setOkButtonText(request.getOkButtonText().trim());
            request.setOkButtonTextTransliterated(request.getOkButtonTextTransliterated().trim());
        }

        if (isNotEmpty(request.getNoButtonText()) && isNotEmpty(request.getNoButtonTextTransliterated())) {
            request.setNoButtonText(request.getNoButtonText().trim());
            request.setNoButtonTextTransliterated(request.getNoButtonTextTransliterated().trim());
        }

        if (isNotEmpty(request.getCancelButtonText()) && isNotEmpty(request.getCancelButtonTextTransliterated())) {
            request.setCancelButtonText(request.getCancelButtonText().trim());
            request.setCancelButtonTextTransliterated(request.getCancelButtonTextTransliterated().trim());
        }
    }

    /**
     * Checks validity of each button text using a helper method.
     *
     * @param request       {@link SystemMessageEditRequest}
     * @param systemMessage (@link SystemMessage}
     * @return false if any of the button texts is invalid, otherwise true
     */
    private boolean isValidButtonTexts(SystemMessageEditRequest request, SystemMessage systemMessage) {
        boolean isValid = true;
        if (Boolean.TRUE.equals(systemMessage.getOkEnabled())) {
            isValid = isValidButtonText(request.getOkButtonText()) && isValidButtonText(systemMessage.getOkButtonTextTransliterated());
        }

        if (Boolean.TRUE.equals(systemMessage.getNoEnabled())) {
            isValid = isValidButtonText(request.getNoButtonText()) && isValidButtonText(systemMessage.getNoButtonTextTransliterated());
        }

        if (Boolean.TRUE.equals(systemMessage.getCancelEnabled())) {
            isValid = isValidButtonText(request.getCancelButtonText()) && isValidButtonText(systemMessage.getCancelButtonTextTransliterated());
        }

        return isValid;
    }

    /**
     * Checks if the button text is valid.
     *
     * @param buttonText button text
     * @return true if the button text is valid, false otherwise.
     */
    private boolean isValidButtonText(String buttonText) {
        return buttonText != null && buttonText.length() >= 1 && buttonText.length() <= 32;
    }

    /**
     * Checks if the new button names in the request object have the same length
     * as the existing button names in the systemMessage object.
     *
     * @param request       {@link SystemMessageEditRequest}
     * @param systemMessage {@link SystemMessage}
     * @return true if buttons have the same length, false otherwise.
     */
    private boolean canChangeButtonNames(SystemMessageEditRequest request, SystemMessage systemMessage) {
        return canChangeButtonName(request.getOkButtonText(), systemMessage.getOkButtonText())
                && canChangeButtonName(request.getOkButtonTextTransliterated(), request.getOkButtonTextTransliterated())
                && canChangeButtonName(request.getNoButtonText(), systemMessage.getNoButtonText())
                && canChangeButtonName(request.getNoButtonTextTransliterated(), systemMessage.getNoButtonTextTransliterated())
                && canChangeButtonName(request.getCancelButtonText(), systemMessage.getCancelButtonText())
                && canChangeButtonName(request.getCancelButtonTextTransliterated(), systemMessage.getCancelButtonTextTransliterated());
    }

    /**
     * Checks if the new button name has the same length as the existing button name.
     *
     * @param newText      new button text
     * @param existingText existing button text
     * @return true if both button names are null or both are not null and have the same length, false otherwise.
     */
    private boolean canChangeButtonName(String newText, String existingText) {
        boolean bothNull = newText == null && existingText == null;
        boolean bothNotEmpty = StringUtils.isNotBlank(newText) && StringUtils.isNotBlank(existingText);
        return bothNull || bothNotEmpty;
    }

    /**
     * <h1>System Message findByKeys</h1>
     * function takes list of string keys of the system message and finds it in database
     *
     * @param keys list of system message keys
     * @return return list of system message object @return {@link SystemMessageResponse}
     */
    // @Cacheable(value = "system_messages") //TODO enable cash ones cashServer will be working properly
    public List<SystemMessageResponse> findByKeys(List<String> keys) {
        log.debug("Find system message by keys : {}", keys);
        return systemMessageRepository
                .findByKeys(keys)
                .orElseThrow(() -> new DomainEntityNotFoundException("System messages with provided keys do not exist;"));
    }

    /**
     * Creates system message.
     *
     * @param request request with system message data
     * @return created system message
     */
    public SystemMessageResponse create(CreateSystemMessageRequest request) {
        log.debug("Create system message: {}", request.toString());
        trimCreateRequestFields(request);

        if (systemMessageRepository.existsByKey(request.getKeyName())) {
            log.error("System Message with this key %s already exists;".formatted(request.getKeyName().trim()));
            throw new ClientException("System Message with this key %s already exists;".formatted(request.getKeyName().trim()), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTitle(request.getTitle());
        systemMessage.setTitleTransliterated(request.getTitleTransliterated());
        systemMessage.setName(request.getName());
        systemMessage.setKey(request.getKeyName());
        systemMessage.setMessageText(request.getMessageText());
        systemMessage.setMessageTextTransliterated(request.getMessageTextTransliterated());
        systemMessage.setSystemMessageType(request.getSystemMessageType());

        systemMessage.setOkButtonText(StringUtils.defaultIfBlank(request.getOkButtonText(), null));
        systemMessage.setOkButtonTextTransliterated(StringUtils.defaultIfBlank(request.getOkButtonTextTransliterated(), null));
        systemMessage.setNoButtonText(StringUtils.defaultIfBlank(request.getNoButtonText(), null));
        systemMessage.setNoButtonTextTransliterated(StringUtils.defaultIfBlank(request.getNoButtonTextTransliterated(), null));
        systemMessage.setCancelButtonText(StringUtils.defaultIfBlank(request.getCancelButtonText(), null));
        systemMessage.setCancelButtonTextTransliterated(StringUtils.defaultIfBlank(request.getCancelButtonTextTransliterated(), null));

        systemMessage.setOkEnabled(isNotEmpty(request.getOkButtonText()));
        systemMessage.setNoEnabled(isNotEmpty(request.getNoButtonText()));
        systemMessage.setCancelEnabled(isNotEmpty(request.getCancelButtonText()));

        return new SystemMessageResponse(systemMessageRepository.save(systemMessage));
    }

    /**
     * Trims {@link CreateSystemMessageRequest} fields.
     *
     * @param request {@link CreateSystemMessageRequest}
     */
    private void trimCreateRequestFields(CreateSystemMessageRequest request) {
        request.setTitle(request.getTitle().trim());
        request.setTitleTransliterated(request.getTitleTransliterated().trim());
        request.setName(request.getName().trim());
        request.setKeyName(request.getKeyName().trim());
        request.setMessageText(request.getMessageText().trim());
        request.setMessageTextTransliterated(request.getMessageTextTransliterated().trim());

        if (isNotEmpty(request.getOkButtonText()) && isNotEmpty(request.getOkButtonTextTransliterated())) {
            request.setOkButtonText(request.getOkButtonText().trim());
            request.setOkButtonTextTransliterated(request.getOkButtonTextTransliterated().trim());
        }

        if (isNotEmpty(request.getNoButtonText()) && isNotEmpty(request.getNoButtonTextTransliterated())) {
            request.setNoButtonText(request.getNoButtonText().trim());
            request.setNoButtonTextTransliterated(request.getNoButtonTextTransliterated().trim());
        }

        if (isNotEmpty(request.getCancelButtonText()) && isNotEmpty(request.getCancelButtonTextTransliterated())) {
            request.setCancelButtonText(request.getCancelButtonText().trim());
            request.setCancelButtonTextTransliterated(request.getCancelButtonTextTransliterated().trim());
        }
    }


    /**
     * Lists system messages optionally filtered by name.
     *
     * @param prompt name of the system message
     * @return page of {@link SystemMessageResponse}
     */
    public Page<SystemMessageResponse> list(String prompt, int page, int size) {
        log.debug("List system messages by name: {}", prompt);
        return systemMessageRepository
                .list(EPBStringUtils.fromPromptToQueryParameter(prompt), PageRequest.of(page, size))
                .map(SystemMessageResponse::new);
    }


    /**
     * Lists system messages optionally filtered by id.
     *
     * @param prompt id
     * @return page of {@link SystemMessageResponse}
     */
    public Page<SystemMessageResponse> listById(String prompt, int page, int size) {
        log.debug("List system messages by id: {}", prompt);
        return systemMessageRepository
                .listById(prompt, PageRequest.of(page, size))
                .map(SystemMessageResponse::new);
    }
}

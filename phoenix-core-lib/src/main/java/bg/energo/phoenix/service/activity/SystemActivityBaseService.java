package bg.energo.phoenix.service.activity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivity;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.activity.BaseSystemActivityRequest;
import bg.energo.phoenix.model.request.activity.CreateSystemActivityRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityJsonFieldResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.repository.activity.SystemActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ActivityRepository;
import bg.energo.phoenix.repository.nomenclature.contract.SubActivityRepository;
import bg.energo.phoenix.service.activity.validator.FieldTypeValidatorFactory;
import bg.energo.phoenix.service.activity.validator.SystemActivityFieldTypeValidator;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static bg.energo.phoenix.model.enums.nomenclature.SubActivityFieldType.*;
import static bg.energo.phoenix.util.epb.EPBFinalFields.SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER;

@Slf4j
@AllArgsConstructor
public abstract class SystemActivityBaseService {

    protected ActivityRepository activityRepository;
    protected SubActivityRepository subActivityRepository;
    protected SystemActivityRepository systemActivityRepository;
    protected NomenclatureService nomenclatureService;
    protected SystemActivityFileService systemActivityFileService;


    /**
     * @return the type of the connection for which the service is responsible
     */
    public abstract SystemActivityConnectionType getActivityConnectionType();


    /**
     * @param connectedObjectId the ID of the connected object
     * @return the list of activity numbers for the given connected object
     */
    public abstract List<SystemActivityShortResponse> getActivitiesByConnectedObjectId(Long connectedObjectId);


    /**
     * Creates an activity if all validations pass.
     * The created entity should be used by the children classes to create the relevant connection entity.
     *
     * @param request the request to create the activity with
     * @return the ID of the created activity
     */
    @Transactional
    public Long create(CreateSystemActivityRequest request, SystemActivityConnectionType connectionType) {
        log.debug("Creating activity for contract with request: {}", request);

        Activity activity = activityRepository
                .findByIdAndStatusIn(request.getActivityId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("activityId-Activity not found by ID %s and status in: %s;"
                        .formatted(request.getActivityId(), List.of(NomenclatureItemStatus.ACTIVE))));

        SubActivity subActivity = subActivityRepository
                .findByIdAndStatusIn(request.getSubActivityId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("subActivityId-SubActivity not found by ID %s and status in: %s;"
                        .formatted(request.getSubActivityId(), List.of(NomenclatureItemStatus.ACTIVE))));

        if (!subActivity.getActivity().getId().equals(activity.getId())) {
            log.error("subActivityId-SubActivity with ID {} is not related to Activity with ID {}", subActivity.getId(), activity.getId());
            throw new IllegalArgumentsProvidedException("subActivityId-SubActivity with ID %s is not related to Activity with ID %s;"
                    .formatted(subActivity.getId(), activity.getId()));
        }

        List<String> fieldValidationMessages = new ArrayList<>();

        SystemActivity systemActivity = fromCreateRequestToEntity(request, connectionType, activity, subActivity);
        systemActivityRepository.saveAndFlush(systemActivity);

        validateFieldsOnCreate(systemActivity, subActivity.getFields(), request.getFields(), fieldValidationMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(fieldValidationMessages, log);

        return systemActivity.getId();
    }


    /**
     * Maps the request to an entity.
     *
     * @param request     the request to map containing the fields
     * @param activity    the {@link Activity} nomenclature
     * @param subActivity the {@link SubActivity} nomenclature
     * @return the mapped entity
     */
    private SystemActivity fromCreateRequestToEntity(CreateSystemActivityRequest request,
                                                     SystemActivityConnectionType connectionType,
                                                     Activity activity,
                                                     SubActivity subActivity) {
        Long id = systemActivityRepository.getNextSequenceValue();
        SystemActivity systemActivity = new SystemActivity();
        systemActivity.setId(id);
        systemActivity.setActivityNumber(id);
        systemActivity.setActivityId(activity.getId());
        systemActivity.setSubActivityId(subActivity.getId());
        systemActivity.setStatus(EntityStatus.ACTIVE);
        systemActivity.setConnectionType(connectionType);
        systemActivity.setFields(request.getFields());
        return systemActivity;
    }


    /**
     * Validates the fields of the request against the fields of the sub-activity, and also validates provided values against the constraints.
     *
     * @param sourceFields            the fields of the sub-activity
     * @param requestFields           the fields of the request
     * @param fieldValidationMessages the list of validation messages to be populated
     */
    private void validateFieldsOnCreate(SystemActivity systemActivity,
                                        List<SubActivityJsonField> sourceFields,
                                        List<SystemActivityJsonField> requestFields,
                                        List<String> fieldValidationMessages) {
        if (requestFields.size() != sourceFields.size()) {
            log.error("Number of fields in sub-activity and the provided activity request do not match;");
            throw new IllegalArgumentsProvidedException("Number of fields in sub-activity and the provided activity request do not match;");
        }

        requestFields.sort(Comparator.comparing(SystemActivityJsonField::getOrdering));
        sourceFields.sort(Comparator.comparing(SubActivityJsonField::getOrdering));

        for (int i = 0; i < requestFields.size(); i++) {
            SystemActivityJsonField requestField = requestFields.get(i);
            SubActivityJsonField sourceField = sourceFields.get(i);
            if (isFieldStructureInvalid(fieldValidationMessages, requestField, sourceField, i)) continue;
            if (StringUtils.isNotEmpty(requestField.getSelectedValue())) {
                validateSelectedValueAgainstTheFieldTypeOnCreate(systemActivity, fieldValidationMessages, requestField, i);
            }
        }
    }


    /**
     * Validates the field structure equivalence and mandatory value presence between the request and the source
     *
     * @param fieldValidationMessages the list of validation messages to be populated
     * @param requestField            the request field to be validated
     * @param sourceField             the source field to be validated
     * @param i                       the index of the field
     * @return true if the field structure is invalid, false otherwise
     */
    private boolean isFieldStructureInvalid(List<String> fieldValidationMessages, SystemActivityJsonField requestField, SubActivityJsonField sourceField, int i) {
        validateRequestFieldsStructureEquivalenceToSource(fieldValidationMessages, requestField, sourceField, i);

        String selectedValue = requestField.getSelectedValue();
        if (requestField.isMandatory() && StringUtils.isEmpty(selectedValue)) {
            log.error("fields[%s].selectedValue-Filling in the field is mandatory;".formatted(i));
            fieldValidationMessages.add("fields[%s].selectedValue-Field is mandatory;".formatted(i));
            return true;
        }

        return false;
    }


    /**
     * Validates the selected value against the field type
     *
     * @param fieldValidationMessages the list of validation messages to be populated
     * @param requestField            the request field to be validated
     */
    private void validateSelectedValueAgainstTheFieldTypeOnCreate(SystemActivity systemActivity,
                                                                  List<String> fieldValidationMessages,
                                                                  SystemActivityJsonField requestField,
                                                                  int index) {
        // here we know for sure, that the selected value is not empty
        String selectedValue = requestField.getSelectedValue();
        SystemActivityFieldTypeValidator validator = FieldTypeValidatorFactory.getValidator(
                systemActivity,
                requestField.getFieldType(),
                nomenclatureService,
                systemActivityFileService
        );
        validator.validateOnCreate(selectedValue, requestField, fieldValidationMessages, index);
    }


    /**
     * Validates that the request fields are equivalent to the source fields
     *
     * @param fieldValidationMessages the list of validation messages to be populated
     * @param requestField            the request field to be validated
     * @param field                   the source field to be validated
     */
    private void validateRequestFieldsStructureEquivalenceToSource(List<String> fieldValidationMessages,
                                                                   SystemActivityJsonField requestField,
                                                                   SubActivityJsonField field,
                                                                   int index) {
        if (!Objects.equals(requestField.getTitle(), field.getTitle())) {
            log.error("fields[%s].title-Provided field and sub-activity field titles mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].title-Provided field and sub-activity field titles mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.getFieldType(), field.getFieldType())) {
            log.error("fields[%s].fieldType-Provided field and sub-activity field types mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].fieldType-Provided field and sub-activity field types mismatch;".formatted(index));
        }

        if (requestField.getRegexp().size() != field.getRegexp().size() || !CollectionUtils.containsAll(requestField.getRegexp(), field.getRegexp())) {
            log.error("fields[%s].regexp-Provided field and sub-activity field regexps mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].regexp-Provided field and sub-activity field regexps mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.getOrdering(), field.getOrdering())) {
            log.error("fields[%s].ordering-Provided field and sub-activity field orderings mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].ordering-Provided field and sub-activity field orderings mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.isMandatory(), field.isMandatory())) {
            log.error("fields[%s].mandatory-Provided field and sub-activity field mandatory mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].mandatory-Provided field and sub-activity field mandatory mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.isDefaultValue(), field.isDefaultValue())) { // means "hasDefaultValue"
            log.error("fields[%s].defaultValue-Provided field and sub-activity field default value mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].defaultValue-Provided field and sub-activity field default value mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.getValue(), field.getValue())) {
            log.error("fields[%s].value-Provided field and sub-activity field value mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].value-Provided field and sub-activity field value mismatch;".formatted(index));
        }

        if (!Objects.equals(requestField.getMaxLength(), field.getMaxLength())) {
            log.error("fields[%s].maxLength-Provided field and sub-activity field max length mismatch;".formatted(index));
            fieldValidationMessages.add("fields[%s].maxLength-Provided field and sub-activity field max length mismatch;".formatted(index));
        }
    }


    /**
     * Edits an activity if all validations pass.
     *
     * @param id      the ID of the activity to be edited
     * @param request the request to edit the activity with
     * @return the ID of the edited activity
     */
    @Transactional
    public Long edit(Long id, BaseSystemActivityRequest request) {
        log.debug("Editing activity with ID {} with request {};", id, request);

        SystemActivity systemActivity = systemActivityRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found by ID %s and status in: %s;".formatted(id, List.of(EntityStatus.ACTIVE))));

        List<SystemActivityJsonField> fields = systemActivity.getFields(); // the "original" fields
        List<SystemActivityJsonField> requestFields = request.getFields();

        List<String> fieldValidationMessages = new ArrayList<>();
        validateFieldsOnEdit(systemActivity, fields, requestFields, fieldValidationMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(fieldValidationMessages, log);
        systemActivityRepository.editActivity(id, requestFields);
        return id;
    }


    /**
     * Validates the fields on edit
     *
     * @param fields        the persisted fields of the activity to be edited
     * @param requestFields the fields of the request to edit the activity with
     */
    private void validateFieldsOnEdit(SystemActivity systemActivity,
                                      List<SystemActivityJsonField> fields,
                                      List<SystemActivityJsonField> requestFields,
                                      List<String> fieldValidationMessages) {
        if (requestFields.size() != fields.size()) {
            log.error("Number of fields in sub-activity and the provided activity request do not match;");
            throw new IllegalArgumentsProvidedException("Number of fields in sub-activity and the provided activity request do not match;");
        }

        requestFields.sort(Comparator.comparing(SystemActivityJsonField::getOrdering));
        fields.sort(Comparator.comparing(SubActivityJsonField::getOrdering));

        for (int i = 0; i < requestFields.size(); i++) {
            SystemActivityJsonField field = fields.get(i);
            SystemActivityJsonField requestField = requestFields.get(i);
            if (isFieldStructureInvalid(fieldValidationMessages, requestField, field, i)) continue;
            if (StringUtils.isNotEmpty(requestField.getSelectedValue())) {
                validateSelectedValueAgainstTheFieldTypeOnEdit(systemActivity, fieldValidationMessages, requestField, field.getSelectedValue(), i);
            }
        }
    }


    /**
     * Validates the selected value against the field type
     *
     * @param fieldValidationMessages the list of validation messages to be populated
     * @param requestField            the request field to be validated
     */
    private void validateSelectedValueAgainstTheFieldTypeOnEdit(SystemActivity systemActivity,
                                                                List<String> fieldValidationMessages,
                                                                SystemActivityJsonField requestField,
                                                                String persistedValue,
                                                                int index) {
        // here we know for sure, that the selected value is not empty
        String selectedValue = requestField.getSelectedValue();
        SystemActivityFieldTypeValidator validator = FieldTypeValidatorFactory.getValidator(
                systemActivity,
                requestField.getFieldType(),
                nomenclatureService,
                systemActivityFileService
        );
        validator.validateOnEdit(persistedValue, selectedValue, requestField, fieldValidationMessages, index);
    }


    /**
     * @param id the ID of the activity to be viewed
     * @return the activity with the provided ID which will be further populated by the children classes
     */
    public SystemActivityResponse view(Long id) {
        log.debug("Viewing activity with ID {};", id);

        SystemActivity systemActivity = systemActivityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found by ID %s;".formatted(id)));

        Activity activity = activityRepository
                .findById(systemActivity.getActivityId())
                .orElseThrow(() -> new DomainEntityNotFoundException("activityId-Activity not found by ID %s;".formatted(systemActivity.getActivityId())));

        SubActivity subActivity = subActivityRepository
                .findById(systemActivity.getSubActivityId())
                .orElseThrow(() -> new DomainEntityNotFoundException("subActivityId-SubActivity not found by ID %s;".formatted(systemActivity.getSubActivityId())));

        SystemActivityResponse response = new SystemActivityResponse();
        response.setId(systemActivity.getId());
        response.setActivityId(systemActivity.getActivityId());
        response.setActivityName(activity.getName());
        response.setSubActivityId(subActivity.getId());
        response.setSubActivityName(subActivity.getName());
        response.setConnectionType(systemActivity.getConnectionType());
        response.setStatus(systemActivity.getStatus());

        // process field values
        List<SystemActivityJsonFieldResponse> fieldResponses = new ArrayList<>();
        List<SystemActivityJsonField> persistedFields = systemActivity.getFields();
        for (SystemActivityJsonField persistedField : persistedFields) {
            SystemActivityJsonFieldResponse fieldResponse = new SystemActivityJsonFieldResponse(persistedField);
            if (persistedField.getFieldType().equals(NOMENCLATURE_SINGLE_SELECT_DROPDOWN) || persistedField.getFieldType().equals(NOMENCLATURE_MULTI_SELECT_DROPDOWN)) {
                // if the field is a nomenclature field, getting nomenclature values should be delegated to the nomenclature service
                if (StringUtils.isNotEmpty(persistedField.getSelectedValue())) {
                    List<ActivityNomenclatureResponse> nomenclatures = nomenclatureService.findByIdIn(
                            persistedField.getValue(),
                            Arrays.stream(persistedField.getSelectedValue().split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).map(Long::parseLong).toList()
                    );
                    fieldResponse.setNomenclatureValues(nomenclatures);
                }
            }

            if (persistedField.getFieldType().equals(FILE)) {
                if (StringUtils.isNotEmpty(persistedField.getSelectedValue())) {
                    List<Long> fileIds = Arrays
                            .stream(persistedField.getSelectedValue().split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER))
                            .map(Long::parseLong)
                            .toList();
                    fieldResponse.setFiles(systemActivityFileService.getFilesByIdsIn(fileIds));
                }
            }

            fieldResponses.add(fieldResponse);
        }

        // order fields by their ordering
        fieldResponses.sort(Comparator.comparing(SystemActivityJsonFieldResponse::getOrdering));
        response.setFields(fieldResponses);

        return response;
    }


    /**
     * Sets deleted status to the activity if all validations pass
     *
     * @param id the ID of the activity to be deleted
     * @return the ID of the deleted activity
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting activity with ID {};", id);

        SystemActivity systemActivity = systemActivityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Activity not found by ID %s;".formatted(id)));

        if (systemActivity.getStatus().equals(EntityStatus.DELETED)) {
            log.error("id-Activity with ID {} is already deleted;", id);
            throw new OperationNotAllowedException("id-Activity with ID %s is already deleted;".formatted(id));
        }

        systemActivity.setStatus(EntityStatus.DELETED);
        systemActivityRepository.save(systemActivity);

        return id;
    }
}

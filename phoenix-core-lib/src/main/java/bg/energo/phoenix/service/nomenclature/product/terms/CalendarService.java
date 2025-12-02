package bg.energo.phoenix.service.nomenclature.product.terms;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.terms.CalendarRequest;
import bg.energo.phoenix.model.request.nomenclature.product.terms.HolidaysRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.HolidayResponse;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.*;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CALENDAR;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService implements NomenclatureBaseService {
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CALENDAR;
    }

    /**
     * Filters {@link Calendar} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable fields are:
     * <ul>
     *     <li>{@link Calendar#name}</li>
     * </ul>
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CALENDAR, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Calendar list with request: {}", request);
        return calendarRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Calendar} item in the calendar list to a specified position.
     * The method retrieves the {@link Calendar} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Calendar} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Calendar} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CALENDAR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of calendar item with ID: {} to place {}", request.getId(), request.getOrderingId());

        Calendar calendar = calendarRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar not found"));

        Long start;
        Long end;
        List<Calendar> calendars;

        if (calendar.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = calendar.getOrderingId();
            calendars = calendarRepository.findInOrderingIdRange(start, end, calendar.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Calendar c : calendars) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = calendar.getOrderingId();
            end = request.getOrderingId();
            calendars = calendarRepository.findInOrderingIdRange(start, end, calendar.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Calendar b : calendars) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        calendar.setOrderingId(request.getOrderingId());
        calendars.add(calendar);
        calendarRepository.saveAll(calendars);
    }

    /**
     * Sorts all {@link Calendar} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CALENDAR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the calendars alphabetically");
        List<Calendar> calendars = calendarRepository.orderByName();
        long orderingId = 1;

        for (Calendar m : calendars) {
            m.setOrderingId(orderingId);
            orderingId++;
        }

        calendarRepository.saveAll(calendars);
    }

    /**
     * Deletes {@link Calendar} if the validations are passed.
     *
     * @param id ID of the {@link Calendar}
     * @throws DomainEntityNotFoundException if {@link Calendar} is not found.
     * @throws OperationNotAllowedException  if the {@link Calendar} is already deleted.
     */

    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CALENDAR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing calendar with ID: {}", id);
        Calendar calendar = calendarRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar not found"));

        if (calendar.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        Optional<String> activeConnection = calendarRepository.activeConnection(id);

        if (activeConnection.isPresent()) {
            log.error("Can't delete the nomenclature because it is connected to {}", activeConnection.get());
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to %s".formatted(activeConnection.get()));
        }

        calendar.setStatus(DELETED);
        calendar.setDefaultSelection(false);
        calendarRepository.save(calendar);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return calendarRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return calendarRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link Calendar} at the end with the highest ordering ID.
     * If the request asks to save {@link Calendar} as a default and a default {@link Calendar} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link CalendarRequest}
     * @return {@link CalendarResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public CalendarResponse add(CalendarRequest request) {
        request.setName(request.getName().trim());

        log.debug("Adding Calendar: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        long countOfCalendarsWithName = calendarRepository.countByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (countOfCalendarsWithName > 0) {
            log.error("Cannot add item with name {}", request.getName());
            throw new OperationNotAllowedException(String.format("name-Cannot add item with name [%s], calendar with same name already exists", request.getName()));
        }

        Long lastSortOrder = calendarRepository.findLastOrderingId();
        Calendar calendar = new Calendar(request);
        calendar.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, calendar);
        Calendar savedCalendar = calendarRepository.save(calendar);

        return new CalendarResponse(savedCalendar, saveHolidaysAndReturnList(request, calendar));
    }

    /**
     * Retrieves detailed information about {@link CalendarResponse} by ID
     *
     * @param id ID of {@link Calendar}
     * @return {@link CalendarResponse}
     * @throws DomainEntityNotFoundException if no {@link Calendar} was found with the provided ID.
     */
    public CalendarResponse view(Long id) {
        log.debug("Fetching Calendar with ID: {}", id);
        Calendar calendar = calendarRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Calendar with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        List<Holiday> calendarHolidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendar.getId(), List.of(HolidayStatus.ACTIVE));
        return new CalendarResponse(calendar, calendarHolidays);
    }

    /**
     * Filters {@link Calendar} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Calendar}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<CalendarResponse> Page&lt;CalendarResponse&gt;} containing detailed information
     */
    public Page<CalendarResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Calendar list with request: {}", request.toString());
        Page<CalendarResponse> page = calendarRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        page.forEach(calendarResponse ->
                calendarResponse.setHolidays(holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarResponse.getId(), List.of(HolidayStatus.ACTIVE))
                        .stream()
                        .map(HolidayResponse::new)
                        .toList()));

        return page;
    }

    /**
     * Edit the requested {@link Calendar}.
     * If the request asks to save {@link Calendar} as a default and a default {@link Calendar} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Calendar}
     * @param request {@link CalendarRequest}
     * @return {@link CalendarResponse}
     * @throws DomainEntityNotFoundException if {@link Calendar} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Calendar} is deleted.
     */
    @Transactional
    public CalendarResponse edit(Long id, CalendarRequest request) {
        request.setName(request.getName().trim());
        log.debug("Editing Calendar: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Calendar calendar = calendarRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Calendar with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        if (calendarRepository.countByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !calendar.getName().equals(request.getName().trim())) {
            throw new OperationNotAllowedException(String.format("name-Cannot edit item with name [%s], calendar with same name already exists", request.getName()));
        }

        if (calendar.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, calendar);
        calendar.updateCalendar(request);

        return new CalendarResponse(calendarRepository.save(calendar), updateHolidaysAndReturnList(request, calendar));
    }

    private List<Holiday> saveHolidaysAndReturnList(CalendarRequest request, Calendar calendar) {
        List<HolidaysRequest> holidaysRequestList = request.getHolidays();
        if (holidaysRequestList == null) {
            return new ArrayList<>();
        }
        Set<LocalDate> uniqueLocalDates = new HashSet<>();
        List<Holiday> savedHolidayList = new ArrayList<>();

        for (HolidaysRequest holidaysRequest : holidaysRequestList) {
            if (!uniqueLocalDates.add(holidaysRequest.getHolidayDate())) {
                throw new ClientException(String.format("holidays.holidayDate-You cant add holiday with same date [%s] twice", holidaysRequest.getHolidayDate().toString()), APPLICATION_ERROR);
            }

            if (holidaysRequest.getHolidayStatus().equals(HolidayStatus.DELETED)) {
                throw new ClientException("holidays.holidayStatus-You cant add holiday with status DELETED", APPLICATION_ERROR);
            }

            savedHolidayList.add(holidaysRepository.save(new Holiday(calendar.getId(), holidaysRequest)));
        }

        return savedHolidayList;
    }

    private List<Holiday> updateHolidaysAndReturnList(CalendarRequest request, Calendar calendar) {
        List<HolidaysRequest> holidaysRequestList = request.getHolidays();
        if (holidaysRequestList == null) {
            return new ArrayList<>();
        }
        Set<LocalDate> uniqueLocalDates = new HashSet<>();
        List<Holiday> updatedHolidayList = new ArrayList<>();

        for (HolidaysRequest holidaysRequest : holidaysRequestList) {
            if (!uniqueLocalDates.add(holidaysRequest.getHolidayDate())) {
                throw new ClientException(String.format("holidays.holidayDate-You cant add holiday with same date [%s] twice", holidaysRequest.getHolidayDate().toString()), APPLICATION_ERROR);
            }

            List<Holiday> allByCalendarIdAndHolidayStatusAndHolidayDate = holidaysRepository.findAllByCalendarIdAndHolidayStatusAndHolidayDate(calendar.getId(), List.of(HolidayStatus.ACTIVE), holidaysRequest.getHolidayDate().atStartOfDay());
            if (holidaysRequest.getId() != null) {
                if (!allByCalendarIdAndHolidayStatusAndHolidayDate.isEmpty()) {
                    Optional<Holiday> holidayByCalendarIdStatusAndDateOptional = allByCalendarIdAndHolidayStatusAndHolidayDate.stream().filter(holiday -> holiday.getId().equals(holidaysRequest.getId())).findAny();
                    if (holidayByCalendarIdStatusAndDateOptional.isPresent()) {
                        Holiday holiday = holidayByCalendarIdStatusAndDateOptional.get();
                        if (!holiday.getId().equals(holidaysRequest.getId())) {
                            throw new ClientException(String.format("holidays.holidayDate-Holiday with presented date [%s] already assigned to Calendar, cannot add new one", holidaysRequest.getHolidayDate().toString()), APPLICATION_ERROR);
                        }
                    }
                }

                Optional<Holiday> holidayOptional = holidaysRepository.findByIdAndStatus(holidaysRequest.getId(), List.of(HolidayStatus.ACTIVE));
                if (holidayOptional.isEmpty()) {
                    throw new ClientException(String.format("holidays.id-Holiday with presented id [%s] not found, cannot edit", holidaysRequest.getId()), DOMAIN_ENTITY_NOT_FOUND);
                } else {
                    Holiday holiday = holidayOptional.get();
                    holiday.setHoliday(holidaysRequest.getHolidayDate().atStartOfDay());
                    holiday.setHolidayStatus(holidaysRequest.getHolidayStatus());

                    updatedHolidayList.add(holiday);
                }
            } else {
                if (!allByCalendarIdAndHolidayStatusAndHolidayDate.isEmpty()) {
                    throw new ClientException(String.format("holidays.holidayDate-Holiday with presented date [%s] already assigned to Calendar, cannot add new one", holidaysRequest.getHolidayDate().toString()), APPLICATION_ERROR);
                }
                updatedHolidayList.add(new Holiday(calendar.getId(), holidaysRequest));
            }
        }

        List<Holiday> allByCalendarIdAndHolidayStatus = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendar.getId(), List.of(HolidayStatus.ACTIVE));
        allByCalendarIdAndHolidayStatus.stream()
                .filter(existingHoliday -> updatedHolidayList.stream()
                        .noneMatch(updatedHolidayListEntities ->
                                existingHoliday.getId().equals(updatedHolidayListEntities.getId())
                        )
                ).toList().forEach(outdateHoliday -> {
                    outdateHoliday.setHolidayStatus(HolidayStatus.DELETED);
                    holidaysRepository.save(outdateHoliday);
                });
        holidaysRepository.saveAll(updatedHolidayList);

        return updatedHolidayList;
    }

    private void checkCurrentDefaultSelection(CalendarRequest request, Calendar calendar) {
        if (request.getStatus().equals(INACTIVE)) {
            calendar.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<Calendar> currentDefaultCalendarOptional = calendarRepository.findByDefaultSelectionTrue();
                if (currentDefaultCalendarOptional.isPresent()) {
                    Calendar defaultCalendar = currentDefaultCalendarOptional.get();
                    defaultCalendar.setDefaultSelection(false);
                    calendarRepository.save(defaultCalendar);
                }
            }
            calendar.setDefaultSelection(request.getDefaultSelection());
        }
    }
}

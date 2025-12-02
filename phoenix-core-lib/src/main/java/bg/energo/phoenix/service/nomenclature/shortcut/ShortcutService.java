package bg.energo.phoenix.service.nomenclature.shortcut;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.nomenclature.shortcut.Shortcut;
import bg.energo.phoenix.model.enums.nomenclature.shortcut.UserShortcuts;
import bg.energo.phoenix.model.request.nomenclature.shortcut.ShortcutOrderChangeRequest;
import bg.energo.phoenix.model.request.nomenclature.shortcut.UserShortcutRequest;
import bg.energo.phoenix.model.response.nomenclature.shortcut.UserShortcutResponse;
import bg.energo.phoenix.repository.nomenclature.shortcut.ShortcutRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortcutService {

    private final ShortcutRepository shortcutRepository;
    private final PermissionService permissionService;


    public List<UserShortcutResponse> listShortcuts() {
        return shortcutRepository.findAllByUsername(permissionService.getLoggedInUserId()).stream().map(UserShortcutResponse::new).toList();
    }

    @Transactional
    public void add(UserShortcutRequest request) {
        UserShortcuts shortcut = request.getShortcut();
        String loggedInUserId = permissionService.getLoggedInUserId();
        if (shortcutRepository.existsByUsernameAndShortcut(loggedInUserId, shortcut)) {
            log.error("shortcut-Shortcut does not exist!;");
            throw new OperationNotAllowedException("shortcut-Shortcut already exist!;");
        }
        Long lastOrderingId = shortcutRepository.findLastOrderingId(loggedInUserId);
        shortcutRepository.save(new Shortcut(null, loggedInUserId, shortcut, lastOrderingId == null ? 1 : lastOrderingId + 1));
    }

    @Transactional
    public void delete(UserShortcutRequest request) {
        UserShortcuts shortcut = request.getShortcut();
        if (!shortcutRepository.existsByUsernameAndShortcut(permissionService.getLoggedInUserId(), shortcut)) {
            log.error("shortcut-Shortcut does not exist!;");
            throw new OperationNotAllowedException("shortcut-Shortcut does not exist!;");
        }
        shortcutRepository.deleteByUsernameAndShortcut(permissionService.getLoggedInUserId(), shortcut);
    }
@Transactional
    public void changeOrder(ShortcutOrderChangeRequest request){

        Shortcut bank = shortcutRepository
                .findByUsernameAndShortcut(permissionService.getLoggedInUserId(), request.getShortcuts())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Bank not found"));

        Long start;
        Long end;
        List<Shortcut> banks;

        if (bank.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = bank.getOrderingId();
            banks = shortcutRepository.findInOrderingIdRange(start, end, bank.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Shortcut b : banks) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = bank.getOrderingId();
            end = request.getOrderingId();
            banks = shortcutRepository.findInOrderingIdRange(start, end, bank.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Shortcut b : banks) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        bank.setOrderingId(request.getOrderingId());
        banks.add(bank);
        shortcutRepository.saveAll(banks);
    }
}

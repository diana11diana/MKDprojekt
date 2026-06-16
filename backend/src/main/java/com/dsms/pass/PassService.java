package com.dsms.pass;

import com.dsms.auth.AuthException;
import com.dsms.notification.NotificationService;
import com.dsms.notification.NotificationType;
import com.dsms.pass.PassDtos.*;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PassService {

    private final PassTypeRepository passTypeRepository;
    private final UserPassRepository userPassRepository;
    private final PassLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PassService(
            PassTypeRepository passTypeRepository,
            UserPassRepository userPassRepository,
            PassLedgerRepository ledgerRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.passTypeRepository = passTypeRepository;
        this.userPassRepository = userPassRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PassTypeResponse> listPublicTypes() {
        return passTypeRepository.findByActiveTrueOrderByPriceAsc()
                .stream().map(PassTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PassTypeResponse> listAllTypes() {
        return passTypeRepository.findAllByOrderByPriceAsc()
                .stream().map(PassTypeResponse::from).toList();
    }

    @Transactional
    public PassTypeResponse createType(PassTypeRequest request) {
        validateType(request.type(), request.visitCount());
        PassType passType = new PassType(
                request.name().trim(),
                normalize(request.description()),
                request.type(),
                request.visitCount(),
                request.validityDays(),
                request.price()
        );
        return PassTypeResponse.from(passTypeRepository.save(passType));
    }

    @Transactional
    public PassTypeResponse updateType(Long id, PassTypeRequest request) {
        validateType(request.type(), request.visitCount());
        PassType passType = getType(id);
        passType.update(
                request.name().trim(),
                normalize(request.description()),
                request.type(),
                request.visitCount(),
                request.validityDays(),
                request.price(),
                request.active()
        );
        return PassTypeResponse.from(passType);
    }

    @Transactional
    public UserPassResponse grantPass(GrantPassRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        PassType passType = getType(request.passTypeId());
        Instant validFrom = request.validFrom() == null ? Instant.now() : request.validFrom();
        UserPass pass = createUserPass(user, passType, validFrom, "Pass granted by administrator");
        notificationService.notify(
                user,
                NotificationType.PASS_GRANTED,
                "Абонемент выдан",
                "Администратор выдал вам абонемент \"" + passType.getName() + "\"."
        );
        return UserPassResponse.from(pass);
    }

    @Transactional(readOnly = true)
    public List<UserPassResponse> listMyPasses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        return userPassRepository.findByUserIdOrderByValidUntilDesc(user.getId())
                .stream().map(UserPassResponse::from).toList();
    }

    public UserPass reserveUsablePass(Long userId, Instant classTime) {
        return userPassRepository.findUsableCandidatesForUpdate(userId, classTime)
                .stream()
                .filter(pass -> pass.canUseAt(classTime))
                .findFirst()
                .orElseThrow(() -> new AuthException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No active pass is valid for this class"
                ));
    }

    public void reserveVisit(UserPass pass, com.dsms.booking.Reservation reservation) {
        pass.reserveVisit();
        ledgerRepository.save(new PassLedgerEntry(
                pass, reservation, LedgerEntryType.RESERVE, -1, "Class reservation"
        ));
    }

    public void releaseVisit(UserPass pass, com.dsms.booking.Reservation reservation) {
        pass.releaseVisit();
        ledgerRepository.save(new PassLedgerEntry(
                pass, reservation, LedgerEntryType.RELEASE, 1, "Reservation cancelled in time"
        ));
    }

    public PassType getActiveType(Long id) {
        PassType passType = getType(id);
        if (!passType.isActive()) {
            throw new AuthException(HttpStatus.CONFLICT, "Pass type is not available");
        }
        return passType;
    }

    public UserPass createUserPass(User user, PassType passType, Instant validFrom, String reason) {
        UserPass pass = userPassRepository.save(new UserPass(user, passType, validFrom));
        ledgerRepository.save(new PassLedgerEntry(
                pass,
                null,
                LedgerEntryType.GRANT,
                pass.getRemainingVisits() == null ? 0 : pass.getRemainingVisits(),
                reason
        ));
        return pass;
    }

    private PassType getType(Long id) {
        return passTypeRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Pass type not found"));
    }

    private void validateType(PassTypeKind type, Integer visitCount) {
        if (type == PassTypeKind.LIMITED && (visitCount == null || visitCount < 1)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Limited pass requires visit count");
        }
        if (type == PassTypeKind.UNLIMITED && visitCount != null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Unlimited pass cannot have visit count");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

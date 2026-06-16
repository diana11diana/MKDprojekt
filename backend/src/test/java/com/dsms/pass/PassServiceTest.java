package com.dsms.pass;

import com.dsms.auth.AuthException;
import com.dsms.notification.NotificationService;
import com.dsms.pass.PassDtos.PassTypeRequest;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PassServiceTest {

    @Mock
    private PassTypeRepository passTypeRepository;

    @Mock
    private UserPassRepository userPassRepository;

    @Mock
    private PassLedgerRepository ledgerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void limitedPassRequiresVisitCount() {
        PassService service = service();
        PassTypeRequest request = new PassTypeRequest(
                "Starter",
                null,
                PassTypeKind.LIMITED,
                null,
                30,
                BigDecimal.valueOf(120),
                true
        );

        assertThatThrownBy(() -> service.createType(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Limited pass requires visit count");
    }

    @Test
    void unlimitedPassCannotHaveVisitCount() {
        PassService service = service();
        PassTypeRequest request = new PassTypeRequest(
                "Unlimited",
                null,
                PassTypeKind.UNLIMITED,
                12,
                30,
                BigDecimal.valueOf(240),
                true
        );

        assertThatThrownBy(() -> service.createType(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Unlimited pass cannot have visit count");
    }

    private PassService service() {
        return new PassService(
                passTypeRepository,
                userPassRepository,
                ledgerRepository,
                userRepository,
                notificationService
        );
    }
}

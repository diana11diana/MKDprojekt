package com.dsms.pass;

import com.dsms.notification.NotificationService;
import com.dsms.pass.PassDtos.CompletePaymentRequest;
import com.dsms.pass.PassDtos.CreatePassOrderRequest;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassOrderServiceTest {

    @Mock
    private PassOrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassService passService;

    @Mock
    private NotificationService notificationService;

    @Test
    void createOrderKeepsPaymentPending() {
        User user = user();
        PassType type = passType();
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(passService.getActiveType(9L)).thenReturn(type);
        when(orderRepository.save(any(PassOrder.class))).thenAnswer(invocation -> {
            PassOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 101L);
            ReflectionTestUtils.setField(order, "createdAt", Instant.now());
            return order;
        });
        PassOrderService service = service();

        PassDtos.PassOrderResponse response =
                service.createOrder("client@example.com", new CreatePassOrderRequest(9L));

        assertThat(response.status()).isEqualTo(PassOrderStatus.PENDING_PAYMENT);
        assertThat(response.amount()).isEqualByComparingTo("160.00");
        verify(notificationService).notify(any(), any(), any(), any());
    }

    @Test
    void completePaymentCreatesUserPassAndMarksOrderPaid() {
        User user = user();
        PassType type = passType();
        PassOrder order = new PassOrder(user, type);
        ReflectionTestUtils.setField(order, "id", 101L);
        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        UserPass userPass = new UserPass(user, type, Instant.now());
        ReflectionTestUtils.setField(userPass, "id", 55L);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(passService.createUserPass(any(), any(), any(), any())).thenReturn(userPass);
        PassOrderService service = service();

        PassDtos.PassOrderResponse response =
                service.completePayment(101L, new CompletePaymentRequest("cash-101"));

        assertThat(response.status()).isEqualTo(PassOrderStatus.PAID);
        assertThat(response.userPassId()).isEqualTo(55L);
        assertThat(response.paymentReference()).isEqualTo("cash-101");
        verify(notificationService).notify(any(), any(), any(), any());
    }

    private PassOrderService service() {
        return new PassOrderService(
                orderRepository,
                userRepository,
                passService,
                notificationService
        );
    }

    private static User user() {
        User user = new User("Client", "One", "client@example.com", null, "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private static PassType passType() {
        PassType type = new PassType(
                "4 classes",
                null,
                PassTypeKind.LIMITED,
                4,
                30,
                BigDecimal.valueOf(160)
        );
        ReflectionTestUtils.setField(type, "id", 9L);
        return type;
    }
}

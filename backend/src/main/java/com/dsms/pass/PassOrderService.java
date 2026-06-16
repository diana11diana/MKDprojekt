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
public class PassOrderService {

    private final PassOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PassService passService;
    private final NotificationService notificationService;

    public PassOrderService(
            PassOrderRepository orderRepository,
            UserRepository userRepository,
            PassService passService,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.passService = passService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PassOrderResponse createOrder(String email, CreatePassOrderRequest request) {
        User user = getUser(email);
        PassType passType = passService.getActiveType(request.passTypeId());
        PassOrder order = orderRepository.save(new PassOrder(user, passType));
        notificationService.notify(
                user,
                NotificationType.PASS_ORDER_CREATED,
                "Заказ создан",
                "Заказ на абонемент \"" + passType.getName() + "\" ожидает оплаты."
        );
        return PassOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<PassOrderResponse> listMine(String email) {
        User user = getUser(email);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(PassOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PassOrderResponse> listAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(PassOrderResponse::from).toList();
    }

    @Transactional
    public PassOrderResponse cancelMine(Long id, String email) {
        User user = getUser(email);
        PassOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Order not found"));
        cancel(order);
        return PassOrderResponse.from(order);
    }

    @Transactional
    public PassOrderResponse cancelByAdmin(Long id) {
        PassOrder order = getOrder(id);
        cancel(order);
        return PassOrderResponse.from(order);
    }

    @Transactional
    public PassOrderResponse completePayment(Long id, CompletePaymentRequest request) {
        PassOrder order = getOrder(id);
        if (!order.isPending()) {
            throw new AuthException(HttpStatus.CONFLICT, "Only pending orders can be paid");
        }
        UserPass pass = passService.createUserPass(
                order.getUser(),
                order.getPassType(),
                Instant.now(),
                "Pass order #" + order.getId() + " paid"
        );
        order.markPaid(pass, normalize(request.paymentReference()));
        notificationService.notify(
                order.getUser(),
                NotificationType.PASS_ORDER_PAID,
                "Оплата подтверждена",
                "Абонемент \"" + order.getPassType().getName() + "\" активирован."
        );
        return PassOrderResponse.from(order);
    }

    private void cancel(PassOrder order) {
        if (!order.isPending()) {
            throw new AuthException(HttpStatus.CONFLICT, "Only pending orders can be cancelled");
        }
        order.cancel();
        notificationService.notify(
                order.getUser(),
                NotificationType.PASS_ORDER_CANCELLED,
                "Заказ отменён",
                "Заказ на абонемент \"" + order.getPassType().getName() + "\" отменён."
        );
    }

    private PassOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.dsms.pass;

import com.dsms.pass.PassDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PassOrderController {

    private final PassOrderService service;

    public PassOrderController(PassOrderService service) {
        this.service = service;
    }

    @PostMapping("/me/pass-orders")
    public PassOrderResponse createMine(
            @Valid @RequestBody CreatePassOrderRequest request,
            Authentication authentication
    ) {
        return service.createOrder(authentication.getName(), request);
    }

    @GetMapping("/me/pass-orders")
    public List<PassOrderResponse> listMine(Authentication authentication) {
        return service.listMine(authentication.getName());
    }

    @DeleteMapping("/me/pass-orders/{id}")
    public ResponseEntity<Void> cancelMine(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.cancelMine(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/pass-orders")
    public List<PassOrderResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/admin/pass-orders/{id}/pay")
    public PassOrderResponse completePayment(
            @PathVariable Long id,
            @Valid @RequestBody CompletePaymentRequest request
    ) {
        return service.completePayment(id, request);
    }

    @PostMapping("/admin/pass-orders/{id}/cancel")
    public PassOrderResponse cancelByAdmin(@PathVariable Long id) {
        return service.cancelByAdmin(id);
    }
}

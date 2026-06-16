package com.dsms.booking;

import com.dsms.booking.BookingDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/classes/{classId}/reservations")
    public BookingResult book(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        return service.book(classId, authentication.getName());
    }

    @DeleteMapping("/classes/{classId}/reservations/me")
    public ResponseEntity<Void> cancel(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        service.cancelReservation(classId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classes/{classId}/waitlist/me")
    public ResponseEntity<Void> leaveWaitingList(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        service.leaveWaitingList(classId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/reservations")
    public List<ReservationResponse> myReservations(Authentication authentication) {
        return service.myReservations(authentication.getName());
    }

    @GetMapping("/me/waitlist")
    public List<WaitingListResponse> myWaitingList(Authentication authentication) {
        return service.myWaitingList(authentication.getName());
    }
}


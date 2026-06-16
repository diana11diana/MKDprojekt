package com.dsms.user;

import com.dsms.auth.AuthDtos.UserResponse;
import com.dsms.auth.AuthException;
import com.dsms.user.UserManagementDtos.UserAdminResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse updateProfile(
            String email,
            String firstName,
            String lastName,
            String phone
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        user.updateProfile(firstName.trim(), lastName.trim(), normalizePhone(phone));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserAdminResponse::from)
                .toList();
    }

    @Transactional
    public UserAdminResponse changeRole(Long id, UserRole role, String actorEmail) {
        User user = getUser(id);
        if (user.getEmail().equals(actorEmail) && role != UserRole.ADMIN) {
            throw new AuthException(HttpStatus.CONFLICT, "You cannot remove your own admin role");
        }
        user.changeRole(role);
        return UserAdminResponse.from(user);
    }

    @Transactional
    public UserAdminResponse changeStatus(Long id, UserStatus status, String actorEmail) {
        User user = getUser(id);
        if (user.getEmail().equals(actorEmail) && status != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.CONFLICT, "You cannot deactivate your own account");
        }
        user.changeStatus(status);
        return UserAdminResponse.from(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}


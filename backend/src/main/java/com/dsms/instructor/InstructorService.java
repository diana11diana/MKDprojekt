package com.dsms.instructor;

import com.dsms.auth.AuthException;
import com.dsms.instructor.InstructorDtos.*;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import com.dsms.user.UserRole;
import com.dsms.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorService {

    private final InstructorProfileRepository profileRepository;
    private final UserRepository userRepository;

    public InstructorService(
            InstructorProfileRepository profileRepository,
            UserRepository userRepository
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> listPublic() {
        return profileRepository.findByPublicProfileTrueOrderByUserLastNameAsc()
                .stream()
                .map(InstructorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> listAll() {
        return profileRepository.findAllByOrderByUserLastNameAsc()
                .stream()
                .map(InstructorResponse::from)
                .toList();
    }

    @Transactional
    public InstructorResponse create(CreateInstructorRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        if (profileRepository.findByUserId(user.getId()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "Instructor profile already exists");
        }

        user.changeRole(UserRole.INSTRUCTOR);
        user.changeStatus(UserStatus.ACTIVE);
        InstructorProfile profile = new InstructorProfile(
                user,
                request.specialization().trim(),
                normalizeDescription(request.description())
        );
        return InstructorResponse.from(profileRepository.save(profile));
    }

    @Transactional
    public InstructorResponse update(Long id, UpdateInstructorRequest request) {
        InstructorProfile profile = getProfile(id);
        profile.update(
                request.specialization().trim(),
                normalizeDescription(request.description()),
                request.publicProfile()
        );
        return InstructorResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public InstructorProfile getProfile(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Instructor not found"));
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}


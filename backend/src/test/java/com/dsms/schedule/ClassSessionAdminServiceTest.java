package com.dsms.schedule;

import com.dsms.instructor.InstructorProfile;
import com.dsms.instructor.InstructorService;
import com.dsms.schedule.ClassSessionDtos.ClassSessionRequest;
import com.dsms.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassSessionAdminServiceTest {

    @Mock
    private ClassSessionRepository repository;

    @Mock
    private InstructorService instructorService;

    @Test
    void newClassIsCreatedAsDraft() {
        User instructorUser = new User(
                "Anna",
                "Kowalska",
                "anna@example.com",
                null,
                "hash"
        );
        InstructorProfile instructor = new InstructorProfile(
                instructorUser,
                "Salsa",
                null
        );
        when(instructorService.getProfile(10L)).thenReturn(instructor);
        when(repository.save(any(ClassSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassSessionAdminService service =
                new ClassSessionAdminService(repository, instructorService);
        ClassSessionRequest request = new ClassSessionRequest(
                "Salsa Start",
                "Basics",
                "Salsa",
                ClassLevel.BEGINNER,
                10L,
                15,
                Instant.now().plusSeconds(3600),
                60
        );

        ClassSessionResponse response = service.create(request);

        assertThat(response.status()).isEqualTo(ClassStatus.DRAFT);
        assertThat(response.instructorName()).isEqualTo("Anna Kowalska");
    }
}


package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.CreateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UpdateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UserDto;
import com.mmiranda.pointsbackapi.exception.DuplicateEmailException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Establishment establishmentA;
    private Establishment establishmentB;

    @BeforeEach
    void setUp() {
        establishmentA = new Establishment();
        establishmentA.setId(1L);
        establishmentB = new Establishment();
        establishmentB.setId(2L);
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void platformAdminCreatesAnyRole() {
        TestAuth.asPlatformAdmin();
        CreateUserRequestDto request = new CreateUserRequestDto("New Owner", "owner@a.com", "pw", Role.ESTABLISHMENT_OWNER, 1L);

        when(userRepository.existsByEmail("owner@a.com")).thenReturn(false);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentA));
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(Role.ESTABLISHMENT_OWNER, result.role());
        assertEquals(1L, result.establishmentId());
    }

    @Test
    void platformAdminCreatingPlatformAdminHasNoEstablishment() {
        TestAuth.asPlatformAdmin();
        CreateUserRequestDto request = new CreateUserRequestDto("New Admin", "admin2@test.com", "pw", Role.PLATFORM_ADMIN, null);

        when(userRepository.existsByEmail("admin2@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.createUser(request);

        assertEquals(Role.PLATFORM_ADMIN, result.role());
        assertEquals(null, result.establishmentId());
        verify(establishmentRepository, never()).findById(any());
    }

    @Test
    void ownerCreatesStaffForOwnEstablishment() {
        TestAuth.asEstablishmentOwner(1L);
        CreateUserRequestDto request = new CreateUserRequestDto("New Staff", "staff@a.com", "pw", Role.ESTABLISHMENT_STAFF, 1L);

        when(userRepository.existsByEmail("staff@a.com")).thenReturn(false);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentA));
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.createUser(request);

        assertEquals(Role.ESTABLISHMENT_STAFF, result.role());
        assertEquals(1L, result.establishmentId());
    }

    @Test
    void ownerCannotCreateStaffForAnotherEstablishment() {
        TestAuth.asEstablishmentOwner(1L);
        CreateUserRequestDto request = new CreateUserRequestDto("New Staff", "staff@b.com", "pw", Role.ESTABLISHMENT_STAFF, 2L);

        assertThrows(ForbiddenException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void ownerCannotCreateNonStaffAccounts() {
        TestAuth.asEstablishmentOwner(1L);
        CreateUserRequestDto request = new CreateUserRequestDto("New Owner", "owner2@a.com", "pw", Role.ESTABLISHMENT_OWNER, 1L);

        assertThrows(ForbiddenException.class, () -> userService.createUser(request));
    }

    @Test
    void staffCannotCreateAnyAccount() {
        TestAuth.asEstablishmentStaff(1L);
        CreateUserRequestDto request = new CreateUserRequestDto("New Staff", "staff2@a.com", "pw", Role.ESTABLISHMENT_STAFF, 1L);

        assertThrows(ForbiddenException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        TestAuth.asPlatformAdmin();
        CreateUserRequestDto request = new CreateUserRequestDto("Dup", "dup@a.com", "pw", Role.ESTABLISHMENT_OWNER, 1L);

        when(userRepository.existsByEmail("dup@a.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void ownerCannotUpdateTheirOwnAccount() {
        TestAuth.asEstablishmentOwner(1L);
        User ownAccount = User.builder().id(2L).role(Role.ESTABLISHMENT_OWNER).establishment(establishmentA).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(ownAccount));

        UpdateUserRequestDto request = new UpdateUserRequestDto("New Name", null, null, Role.PLATFORM_ADMIN, null, null);

        assertThrows(ForbiddenException.class, () -> userService.updateUser(2L, request));
    }

    @Test
    void ownerUpdatesOwnStaff() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).name("Old Name").email("staff@a.com")
                .role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequestDto request = new UpdateUserRequestDto("New Name", null, null, null, null, null);
        UserDto result = userService.updateUser(3L, request);

        assertEquals("New Name", result.name());
    }

    @Test
    void ownerUpdatesStaffEmailPasswordAndActiveFlag() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).name("Old Name").email("staff@a.com").passwordHash("old-hash")
                .role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.existsByEmail("new-staff@a.com")).thenReturn(false);
        when(passwordEncoder.encode("new-pw")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequestDto request = new UpdateUserRequestDto(null, "new-staff@a.com", "new-pw", null, null, false);
        UserDto result = userService.updateUser(3L, request);

        assertEquals("new-staff@a.com", result.email());
        assertEquals(false, result.active());
        assertEquals("new-hash", staff.getPasswordHash());
    }

    @Test
    void updateUserRejectsDuplicateEmail() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).email("staff@a.com")
                .role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.existsByEmail("taken@a.com")).thenReturn(true);

        UpdateUserRequestDto request = new UpdateUserRequestDto(null, "taken@a.com", null, null, null, null);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(3L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRequiresEstablishmentForNonAdminRole() {
        TestAuth.asPlatformAdmin();
        CreateUserRequestDto request = new CreateUserRequestDto("Owner", "owner3@a.com", "pw", Role.ESTABLISHMENT_OWNER, null);

        when(userRepository.existsByEmail("owner3@a.com")).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> userService.createUser(request));
    }

    @Test
    void ownerCannotManageStaffFromAnotherEstablishment() {
        TestAuth.asEstablishmentOwner(1L);
        User otherStaff = User.builder().id(4L).role(Role.ESTABLISHMENT_STAFF).establishment(establishmentB).build();

        when(userRepository.findById(4L)).thenReturn(Optional.of(otherStaff));

        UpdateUserRequestDto request = new UpdateUserRequestDto("New Name", null, null, null, null, null);

        assertThrows(ForbiddenException.class, () -> userService.updateUser(4L, request));
    }

    @Test
    void ownerCannotReassignOwnStaffToAnotherRole() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));

        UpdateUserRequestDto request = new UpdateUserRequestDto(null, null, null, Role.ESTABLISHMENT_OWNER, null, null);

        assertThrows(ForbiddenException.class, () -> userService.updateUser(3L, request));
    }

    @Test
    void ownerCannotMoveOwnStaffToAnotherEstablishment() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));

        UpdateUserRequestDto request = new UpdateUserRequestDto(null, null, null, null, 2L, null);

        assertThrows(ForbiddenException.class, () -> userService.updateUser(3L, request));
    }

    @Test
    void createUserFailsWhenEstablishmentDoesNotExist() {
        TestAuth.asPlatformAdmin();
        CreateUserRequestDto request = new CreateUserRequestDto("Owner", "owner4@a.com", "pw", Role.ESTABLISHMENT_OWNER, 999L);

        when(userRepository.existsByEmail("owner4@a.com")).thenReturn(false);
        when(establishmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.createUser(request));
    }

    @Test
    void getCurrentUserProfileThrowsWhenAccountRecordMissing() {
        TestAuth.asPlatformAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUserProfile());
    }

    @Test
    void platformAdminUpdatesAnyAccountIncludingRole() {
        TestAuth.asPlatformAdmin();
        User staff = User.builder().id(3L).name("Old").email("staff@a.com")
                .role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentA));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequestDto request = new UpdateUserRequestDto(null, null, null, Role.ESTABLISHMENT_OWNER, 1L, null);
        UserDto result = userService.updateUser(3L, request);

        assertEquals(Role.ESTABLISHMENT_OWNER, result.role());
    }

    @Test
    void platformAdminUpdateWithoutRoleOrEstablishmentKeepsExistingOnes() {
        TestAuth.asPlatformAdmin();
        User staff = User.builder().id(3L).name("Old").email("staff@a.com")
                .role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentA));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequestDto request = new UpdateUserRequestDto("New Name", null, null, null, null, null);
        UserDto result = userService.updateUser(3L, request);

        assertEquals("New Name", result.name());
        assertEquals(Role.ESTABLISHMENT_STAFF, result.role());
        assertEquals(1L, result.establishmentId());
    }

    @Test
    void platformAdminUpdateMissingUserThrowsNotFound() {
        TestAuth.asPlatformAdmin();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserRequestDto request = new UpdateUserRequestDto("X", null, null, null, null, null);

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(99L, request));
    }

    @Test
    void staffCannotDeactivateAnyone() {
        TestAuth.asEstablishmentStaff(1L);
        assertThrows(ForbiddenException.class, () -> userService.deactivateUser(3L));
    }

    @Test
    void ownerDeactivatesOwnStaff() {
        TestAuth.asEstablishmentOwner(1L);
        User staff = User.builder().id(3L).role(Role.ESTABLISHMENT_STAFF).establishment(establishmentA).active(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deactivateUser(3L);

        assertEquals(false, staff.isActive());
    }

    @Test
    void platformAdminDeactivatesAnyAccount() {
        TestAuth.asPlatformAdmin();
        User owner = User.builder().id(2L).role(Role.ESTABLISHMENT_OWNER).establishment(establishmentA).active(true).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deactivateUser(2L);

        assertEquals(false, owner.isActive());
    }

    @Test
    void getCurrentUserProfileReturnsCallerProfile() {
        TestAuth.asPlatformAdmin();
        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").role(Role.PLATFORM_ADMIN).active(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        UserDto result = userService.getCurrentUserProfile();

        assertEquals("admin@test.com", result.email());
    }
}

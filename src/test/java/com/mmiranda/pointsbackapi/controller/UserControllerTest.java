package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.CreateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UpdateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UserDto;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDto adminDto() {
        return new UserDto(1L, "Admin", "admin@test.com", Role.PLATFORM_ADMIN, null, true);
    }

    private UserDto staffDto() {
        return new UserDto(3L, "Staff", "staff@a.com", Role.ESTABLISHMENT_STAFF, 1L, true);
    }

    @Test
    void getCurrentUserDelegatesToService() {
        when(userService.getCurrentUserProfile()).thenReturn(adminDto());

        UserDto result = userController.getCurrentUser();

        assertEquals("admin@test.com", result.email());
        verify(userService, times(1)).getCurrentUserProfile();
    }

    @Test
    void listUsersDelegatesToService() {
        when(userService.listUsers()).thenReturn(List.of(adminDto(), staffDto()));

        List<UserDto> result = userController.listUsers();

        assertEquals(2, result.size());
        assertEquals("Admin", result.get(0).name());
        assertEquals("Staff", result.get(1).name());
        verify(userService, times(1)).listUsers();
    }

    @Test
    void createUserDelegatesToService() {
        CreateUserRequestDto request =
                new CreateUserRequestDto("Staff", "staff@a.com", "pw", Role.ESTABLISHMENT_STAFF, 1L);
        when(userService.createUser(any(CreateUserRequestDto.class))).thenReturn(staffDto());

        UserDto result = userController.createUser(request);

        assertNotNull(result);
        assertEquals("staff@a.com", result.email());
        verify(userService, times(1)).createUser(request);
    }

    @Test
    void updateUserDelegatesToService() {
        UpdateUserRequestDto request =
                new UpdateUserRequestDto("New Name", null, null, null, null, null);
        UserDto updated = new UserDto(3L, "New Name", "staff@a.com", Role.ESTABLISHMENT_STAFF, 1L, true);
        when(userService.updateUser(eq(3L), any(UpdateUserRequestDto.class))).thenReturn(updated);

        UserDto result = userController.updateUser(3L, request);

        assertEquals("New Name", result.name());
        verify(userService, times(1)).updateUser(eq(3L), any(UpdateUserRequestDto.class));
    }

    @Test
    void deactivateUserDelegatesToService() {
        userController.deactivateUser(3L);

        verify(userService, times(1)).deactivateUser(3L);
    }
}

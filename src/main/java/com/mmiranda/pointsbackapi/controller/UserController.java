package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.CreateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UpdateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UserDto;
import com.mmiranda.pointsbackapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto getCurrentUser() {
        return userService.getCurrentUserProfile();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')")
    public UserDto createUser(@RequestBody CreateUserRequestDto request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')")
    public UserDto updateUser(@PathVariable Long id, @RequestBody UpdateUserRequestDto request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
    }
}

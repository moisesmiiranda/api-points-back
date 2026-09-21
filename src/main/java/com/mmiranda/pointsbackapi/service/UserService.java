package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.CreateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UpdateUserRequestDto;
import com.mmiranda.pointsbackapi.dto.UserDto;
import com.mmiranda.pointsbackapi.exception.DuplicateEmailException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.mail.EmailTemplates;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {

    private static final String SCOPE_DENIED = "You do not have permission to manage this account";

    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final Clock clock;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository userRepository,
                        EstablishmentRepository establishmentRepository,
                        PasswordEncoder passwordEncoder,
                        PasswordResetService passwordResetService,
                        EmailService emailService,
                        Clock clock,
                        @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.establishmentRepository = establishmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public UserDto getCurrentUserProfile() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(current.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.toDto(user);
    }

    /**
     * Lists accounts the caller may manage:
     * <ul>
     *   <li>{@code PLATFORM_ADMIN}: every account.</li>
     *   <li>{@code ESTABLISHMENT_OWNER}: the {@code ESTABLISHMENT_STAFF} of their own establishment
     *       (the only accounts an owner is allowed to edit or deactivate).</li>
     * </ul>
     * {@code ESTABLISHMENT_STAFF} never reaches this method - the controller restricts it to
     * {@code hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')}.
     */
    public List<UserDto> listUsers() {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        List<User> users = caller.isPlatformAdmin()
                ? userRepository.findAllByOrderByIdAsc()
                : userRepository.findAllByEstablishmentIdAndRoleOrderByIdAsc(
                        caller.establishmentId(), Role.ESTABLISHMENT_STAFF);
        return users.stream()
                .map(UserDto::toDto)
                .toList();
    }

    public UserDto createUser(CreateUserRequestDto request) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();

        if (caller.role() == Role.ESTABLISHMENT_STAFF) {
            throw new ForbiddenException(SCOPE_DENIED);
        }

        if (caller.role() == Role.ESTABLISHMENT_OWNER) {
            if (request.role() != Role.ESTABLISHMENT_STAFF) {
                throw new ForbiddenException(SCOPE_DENIED);
            }
            if (request.establishmentId() == null || !caller.belongsToEstablishment(request.establishmentId())) {
                throw new ForbiddenException(SCOPE_DENIED);
            }
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        Establishment establishment = resolveEstablishment(request.role(), request.establishmentId());

        // No password given: the person is invited by email to choose their own. A password given by the
        // manager is temporary: it has to be replaced at the first access.
        boolean invited = request.password() == null;
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(invited ? randomPassword() : request.password()))
                .role(request.role())
                .establishment(establishment)
                .active(true)
                .mustChangePassword(!invited)
                .build();
        User saved = userRepository.save(user);

        if (invited) {
            passwordResetService.sendInvite(saved);
        } else {
            emailService.sendAsync(EmailTemplates.welcomeWithTemporaryPassword(
                    saved.getEmail(), saved.getName(),
                    establishment != null ? establishment.getName() : null, baseUrl + "/login"));
        }
        return UserDto.toDto(saved);
    }

    public UserDto updateUser(Long targetId, UpdateUserRequestDto request) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        User target = authorizeTargetForManagement(caller, targetId, request.role(), request.establishmentId());

        if (request.name() != null) {
            target.setName(request.name());
        }
        if (request.email() != null && !request.email().equals(target.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateEmailException("An account with this email already exists");
            }
            target.setEmail(request.email());
        }
        if (request.password() != null) {
            // A password set by a manager is temporary, and it ends the person's current sessions
            target.setPasswordHash(passwordEncoder.encode(request.password()));
            target.setMustChangePassword(true);
            target.setPasswordChangedAt(LocalDateTime.now(clock));
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }

        // Role/establishment reassignment is only ever reached here for PLATFORM_ADMIN callers -
        // authorizeTargetForManagement() rejects any attempt by an ESTABLISHMENT_OWNER to change them.
        if (caller.role() == Role.PLATFORM_ADMIN) {
            Role newRole = request.role() != null ? request.role() : target.getRole();
            Long newEstablishmentId = request.establishmentId() != null ? request.establishmentId() : establishmentIdOf(target);
            target.setRole(newRole);
            target.setEstablishment(resolveEstablishment(newRole, newEstablishmentId));
        }

        return UserDto.toDto(userRepository.save(target));
    }

    public void deactivateUser(Long targetId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        User target = authorizeTargetForManagement(caller, targetId, null, null);
        target.setActive(false);
        userRepository.save(target);
    }

    /**
     * Resolves and authorizes the account a PLATFORM_ADMIN or ESTABLISHMENT_OWNER caller is
     * allowed to manage. ESTABLISHMENT_STAFF is always denied. For an ESTABLISHMENT_OWNER,
     * a missing target, a target outside their establishment, a target that isn't STAFF, or a
     * requested role/establishment change all collapse to the same ForbiddenException so a
     * caller can't tell "doesn't exist" from "not yours to manage".
     */
    private User authorizeTargetForManagement(AuthenticatedUser caller, Long targetId,
                                                Role requestedRole, Long requestedEstablishmentId) {
        if (caller.role() == Role.ESTABLISHMENT_STAFF) {
            throw new ForbiddenException(SCOPE_DENIED);
        }

        if (caller.role() == Role.PLATFORM_ADMIN) {
            return userRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        // ESTABLISHMENT_OWNER: target must be their own ESTABLISHMENT_STAFF, and the request
        // must not attempt to move that staff member out of role/establishment.
        User target = userRepository.findById(targetId).orElse(null);
        boolean requestsReassignment = (requestedRole != null && requestedRole != Role.ESTABLISHMENT_STAFF)
                || (requestedEstablishmentId != null && !caller.belongsToEstablishment(requestedEstablishmentId));

        if (target == null
                || target.getRole() != Role.ESTABLISHMENT_STAFF
                || target.getEstablishment() == null
                || !caller.belongsToEstablishment(target.getEstablishment().getId())
                || requestsReassignment) {
            throw new ForbiddenException(SCOPE_DENIED);
        }

        return target;
    }

    private Establishment resolveEstablishment(Role role, Long establishmentId) {
        if (role == Role.PLATFORM_ADMIN) {
            return null;
        }
        if (establishmentId == null) {
            throw new ForbiddenException("An establishment is required for this role");
        }
        return establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find establishment with id: " + establishmentId));
    }

    private Long establishmentIdOf(User user) {
        return user.getEstablishment() != null ? user.getEstablishment().getId() : null;
    }

    /** Long random password nobody knows: the account can only be opened through the invitation link. */
    private String randomPassword() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

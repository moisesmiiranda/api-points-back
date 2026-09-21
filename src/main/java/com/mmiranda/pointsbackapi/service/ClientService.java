package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.dto.AdjustPointsRequestDto;
import com.mmiranda.pointsbackapi.dto.ImportClientRequestDto;
import com.mmiranda.pointsbackapi.dto.LedgerEntryDto;
import com.mmiranda.pointsbackapi.dto.PageDto;
import com.mmiranda.pointsbackapi.dto.RedeemPreviewDto;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Person;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PersonRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import com.mmiranda.pointsbackapi.validation.Documents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PointsService pointsService;

    public ClientDto getClientById(Long id) {
        return ClientDto.toDto(requireScopedClient(id));
    }

    /**
     * Registers the person (found or created by CPF) as a client of the establishment, with a zero
     * balance (use {@link #adjustPoints} to bring in an existing balance). The CPF
     * identifies the person across establishments, but the contact data stored here is this
     * establishment's own record and is never read from or written to another establishment's.
     */
    @Transactional
    public ClientDto createClient(ClientDto clientDto) {
        Establishment establishment = resolveEstablishmentForWrite(clientDto.establishmentId());
        Person person = findOrCreatePerson(clientDto.cpf());
        requireNoAccount(person, establishment);

        Client entity = Client.builder()
                .person(person)
                .name(clientDto.name())
                .email(clientDto.email())
                .phone(clientDto.phone())
                .points(0)
                .establishment(establishment)
                .build();
        return ClientDto.toDto(clientRepository.save(entity));
    }

    /**
     * Clients matching a CPF and/or phone. Non-admin callers only ever search their own
     * establishment; PLATFORM_ADMIN searches every establishment unless one is given.
     */
    public List<ClientDto> searchClients(String cpf, String phone, Long establishmentId) {
        String cpfDigits = blankToNull(Documents.digits(cpf));
        String phoneDigits = blankToNull(Documents.digits(phone));
        if (cpfDigits == null && phoneDigits == null) {
            throw new IllegalArgumentException("Provide a cpf or a phone to search for");
        }

        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long scope = caller.isPlatformAdmin() ? establishmentId : caller.establishmentId();
        return clientRepository.search(scope, cpfDigits, phoneDigits).stream()
                .map(ClientDto::toDto)
                .toList();
    }

    /**
     * Creates the caller's own account for a person who is already a client of another
     * establishment in the same group. Both establishments must have opted in to sharing. Only the
     * contact data is copied; the points balance always starts at zero. A missing person, a
     * non-sharing source and a person unknown to the group all give the same 404.
     */
    @Transactional
    public ClientDto importFromGroup(ImportClientRequestDto request) {
        Establishment target = resolveEstablishmentForWrite(request.establishmentId());
        if (target.getGroup() == null || !target.isShareClients()) {
            throw new ForbiddenException("This establishment does not share clients with a group");
        }

        String cpf = Documents.digits(request.cpf());
        Person person = personRepository.findByCpf(cpf).orElseThrow(ClientService::noSharedClient);
        Client source = clientRepository
                .findSharedAccounts(person.getId(), target.getGroup().getId(), target.getId()).stream()
                .findFirst()
                .orElseThrow(ClientService::noSharedClient);
        requireNoAccount(person, target);

        Client imported = Client.builder()
                .person(person)
                .name(source.getName())
                .email(source.getEmail())
                .phone(source.getPhone())
                .points(0)
                .establishment(target)
                .build();
        return ClientDto.toDto(clientRepository.save(imported));
    }

    private Person findOrCreatePerson(String cpf) {
        String digits = Documents.digits(cpf);
        return personRepository.findByCpf(digits)
                .orElseGet(() -> personRepository.save(Person.builder().cpf(digits).build()));
    }

    private void requireNoAccount(Person person, Establishment establishment) {
        if (person.getId() != null
                && clientRepository.existsByPersonIdAndEstablishmentId(person.getId(), establishment.getId())) {
            throw new ConflictException("A client with this CPF is already registered in this establishment");
        }
    }

    private static ResourceNotFoundException noSharedClient() {
        return new ResourceNotFoundException("No shared client with this CPF was found in your group");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public List<ClientDto> listAllClients() {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        List<Client> clients = caller.isPlatformAdmin()
                ? clientRepository.findAll()
                : clientRepository.findAllByEstablishmentId(caller.establishmentId());
        return clients.stream()
                .map(ClientDto::toDto)
                .toList();
    }

    /**
     * Manual correction of a balance, recorded in the ledger with its reason. The balance can never go
     * below zero. Restricted to owners and admins by the controller.
     */
    @Transactional
    public ClientDto adjustPoints(Long clientId, AdjustPointsRequestDto request) {
        if (request.points() == 0) {
            throw new IllegalArgumentException("points cannot be zero");
        }
        Client client = requireScopedClientForUpdate(clientId);
        pointsService.apply(client, PointsEntryType.ADJUST, request.points(), null, null, request.reason());
        return ClientDto.toDto(client);
    }

    /** The client's points history, newest first. */
    public PageDto<LedgerEntryDto> statement(Long clientId, int page, int size) {
        Client client = requireScopedClient(clientId);
        return pointsService.statement(client.getId(), page, size);
    }

    /** How many points (and how much money) the client may spend on a purchase of {@code amount}. */
    public RedeemPreviewDto redeemPreview(Long clientId, BigDecimal amount) {
        Client client = requireScopedClient(clientId);
        Establishment establishment = client.getEstablishment();
        int balance = client.getPoints() != null ? client.getPoints() : 0;
        boolean discountsEnabled = establishment.getRewardMode() != RewardMode.CATALOG;
        int maxPoints = discountsEnabled ? RewardCalculator.maxPoints(establishment, amount, balance) : 0;
        return new RedeemPreviewDto(
                balance,
                RewardCalculator.discountFor(establishment, balance),
                establishment.getRewardMode(),
                establishment.getPointsToCurrencyRate(),
                maxPoints,
                RewardCalculator.discountFor(establishment, maxPoints));
    }

    @Transactional
    public ClientDto updateClient(Long clientId, ClientDto clientDto) {
        Client clientEntity = requireScopedClientForUpdate(clientId);

        // Update only non-null fields
        if (clientDto.name() != null) {
            clientEntity.setName(clientDto.name());
        }
        if (clientDto.email() != null) {
            clientEntity.setEmail(clientDto.email());
        }
        if (clientDto.phone() != null) {
            clientEntity.setPhone(clientDto.phone());
        }
        if (clientDto.cpf() != null) {
            String newCpf = Documents.digits(clientDto.cpf());
            if (!newCpf.equals(clientEntity.getCpf())) {
                Person person = findOrCreatePerson(newCpf);
                requireNoAccount(person, clientEntity.getEstablishment());
                clientEntity.setPerson(person);
            }
        }
        // Points are not editable here: they only change through purchases, redemptions and adjustPoints.

        return ClientDto.toDto(clientRepository.save(clientEntity));
    }

    /**
     * PLATFORM_ADMIN gets a plain 404 for a missing id. Every other role gets an identical
     * ForbiddenException whether the id belongs to another establishment or doesn't exist at
     * all, so a 403 never leaks which case it was.
     */
    private Client requireScopedClient(Long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        if (caller.isPlatformAdmin()) {
            return clientRepository.findById(id).orElseThrow(() -> clientNotFound(id));
        }
        return clientRepository.findByIdAndEstablishmentId(id, caller.establishmentId())
                .orElseThrow(() -> new ForbiddenException("You do not have access to this client"));
    }

    /** Same visibility rules as {@link #requireScopedClient}, but locks the row for a balance/profile write. */
    private Client requireScopedClientForUpdate(Long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        if (caller.isPlatformAdmin()) {
            return clientRepository.findByIdForUpdate(id).orElseThrow(() -> clientNotFound(id));
        }
        return clientRepository.findByIdForUpdate(id)
                .filter(client -> client.getEstablishment() != null
                        && caller.belongsToEstablishment(client.getEstablishment().getId()))
                .orElseThrow(() -> new ForbiddenException("You do not have access to this client"));
    }

    private static ResourceNotFoundException clientNotFound(Long id) {
        return new ResourceNotFoundException("Cannot find client with id: " + id);
    }

    private Establishment resolveEstablishmentForWrite(Long requestedEstablishmentId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long establishmentId = requestedEstablishmentId;

        if (caller.isPlatformAdmin()) {
            if (establishmentId == null) {
                throw new IllegalArgumentException("establishmentId is required");
            }
        } else if (establishmentId == null) {
            establishmentId = caller.establishmentId();
        } else if (!caller.belongsToEstablishment(establishmentId)) {
            throw new ForbiddenException("You do not have access to this establishment");
        }

        Long resolvedEstablishmentId = establishmentId;
        return establishmentRepository.findById(resolvedEstablishmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find establishment with id: " + resolvedEstablishmentId));
    }
}

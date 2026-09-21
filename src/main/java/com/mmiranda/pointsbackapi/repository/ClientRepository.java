package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByIdAndEstablishmentId(Long id, Long establishmentId);

    List<Client> findAllByEstablishmentId(Long establishmentId);

    boolean existsByPersonIdAndEstablishmentId(Long personId, Long establishmentId);

    Optional<Client> findByPersonCpfAndEstablishmentId(String cpf, Long establishmentId);

    /**
     * Clients of one establishment (or of all, when {@code establishmentId} is null) matching a CPF
     * (digits only) and/or a phone (digits only, compared without punctuation). A null filter is ignored.
     */
    @Query("""
            select c from Client c
            where (:establishmentId is null or c.establishment.id = :establishmentId)
              and (:cpf is null or c.person.cpf = :cpf)
              and (:phone is null or replace(replace(replace(replace(replace(c.phone, '-', ''), ' ', ''), '(', ''), ')', ''), '+', '') = :phone)
            order by c.id
            """)
    List<Client> search(@Param("establishmentId") Long establishmentId,
                        @Param("cpf") String cpf,
                        @Param("phone") String phone);

    /** Accounts of a person held by the other establishments of a group that opted in to sharing. */
    @Query("""
            select c from Client c
            where c.person.id = :personId
              and c.establishment.group.id = :groupId
              and c.establishment.shareClients = true
              and c.establishment.id <> :excludedEstablishmentId
            order by c.id
            """)
    List<Client> findSharedAccounts(@Param("personId") Long personId,
                                    @Param("groupId") Long groupId,
                                    @Param("excludedEstablishmentId") Long excludedEstablishmentId);

    /**
     * Loads the client holding a row lock until the surrounding transaction ends, so concurrent
     * balance changes for the same client are serialized instead of overwriting each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") Long id);
}

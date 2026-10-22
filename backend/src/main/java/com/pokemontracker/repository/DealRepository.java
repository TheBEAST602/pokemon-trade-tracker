package com.pokemontracker.repository;

import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DealRepository extends JpaRepository<Deal, UUID>, JpaSpecificationExecutor<Deal> {

    List<Deal> findByStatusAndDueDateBefore(DealStatus status, LocalDate date);

    List<Deal> findByStatusAndDueDate(DealStatus status, LocalDate date);

    /** Both bounds inclusive. */
    List<Deal> findByStatusAndDueDateBetween(DealStatus status, LocalDate from, LocalDate to);

    long countByStatus(DealStatus status);

    boolean existsByStatusAndSpecialTradeTrueAndCompletedOn(DealStatus status, LocalDate completedOn);

    boolean existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(DealStatus status, LocalDate completedOn, UUID id);
}

package org.example.ainote.repository;

import org.example.ainote.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /** All entries for user, newest first */
    List<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Entries within a createdAt window (for today / date / range queries) */
    List<JournalEntry> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    /** Favourite entries */
    List<JournalEntry> findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long userId);

    /** Safe single-entry lookup with ownership check */
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    /** Ownership check */
    boolean existsByIdAndUserId(Long id, Long userId);
}

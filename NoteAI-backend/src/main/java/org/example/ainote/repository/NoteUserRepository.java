package org.example.ainote.repository;

import org.example.ainote.entity.NoteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteUserRepository extends JpaRepository<NoteUser, Long> {

    Optional<NoteUser> findByUsername(String username);

    /**
     * Check if user owns a note
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM note_users_notes " +
            "WHERE note_id = :noteId AND user_id = :userId",
            nativeQuery = true)
    boolean isNoteOwner(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
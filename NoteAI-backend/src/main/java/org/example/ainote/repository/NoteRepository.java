package org.example.ainote.repository;

import org.example.ainote.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    Optional<Note> findByTitle(String title);


    @Query(value="SELECT n.* FROM notes n JOIN users_notes un ON n.id = un.note_id WHERE un.user_id = :userId ", nativeQuery = true)
    Optional<List<Note>> findAllByUserId(@Param("userId") Long userId);

}

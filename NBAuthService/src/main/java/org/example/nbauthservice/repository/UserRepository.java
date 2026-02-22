package org.example.nbauthservice.repository;


import org.example.nbauthservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findById(Long id);

//    void insertRoles(User user, List<Role> roles);
//    @Modifying не нужен так как я не модифицирую данные а просто читаю

    @Query(value="SELECT COUNT(*) > 0 FROM users_notes WHERE user_id = :userId AND note_id = :noteID", nativeQuery = true)
    boolean isNoteOwner(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<User> findTop20ByUsernameContainingIgnoreCase(String usernamePart);


    void deleteById(Long id);

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
}

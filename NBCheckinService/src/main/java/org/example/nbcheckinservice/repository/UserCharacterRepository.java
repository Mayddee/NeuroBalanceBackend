package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    Optional<UserCharacter> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
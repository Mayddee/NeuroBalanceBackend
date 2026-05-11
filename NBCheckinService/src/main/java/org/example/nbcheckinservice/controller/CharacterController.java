package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.dto.CharacterSelectionRequest;
import org.example.nbcheckinservice.entity.UserCharacter;
import org.example.nbcheckinservice.service.GameProgressionService;
import org.example.nbcheckinservice.service.UserCharacterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Character", description = "User character/pet management and progression")
public class CharacterController {

    private final UserCharacterService characterService;
    private final GameProgressionService gameProgressionService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping
    @Operation(summary = "Get current character state")
    public ResponseEntity<?> getCharacter(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("GET /character - User {}", userId);
        return ResponseEntity.ok(characterService.getCharacterResponse(userId));
    }

    @PostMapping("/select")
    @Operation(summary = "Select initial character (one-time)")
    public ResponseEntity<?> selectCharacter(
            HttpServletRequest request,
            @Validated @RequestBody CharacterSelectionRequest selectionRequest
    ) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("POST /character/select - User {} → {}", userId, selectionRequest.getCharacterType());
        CharacterResponse response = characterService.selectInitialCharacter(userId, selectionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/change-type")
    @Operation(summary = "Change character type")
    public ResponseEntity<?> changeCharacterType(
            HttpServletRequest request,
            @RequestParam UserCharacter.CharacterType characterType
    ) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("POST /character/change-type - User {} → {}", userId, characterType);
        return ResponseEntity.ok(characterService.changeCharacterType(userId, characterType));
    }

    @PostMapping("/feed")
    @Operation(summary = "Feed character (+10 happiness, +15 energy)")
    public ResponseEntity<?> feedCharacter(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("POST /character/feed - User {}", userId);
        return ResponseEntity.ok(characterService.feedCharacter(userId));
    }

    @PostMapping("/play")
    @Operation(summary = "Play with character (+15 happiness, -5 energy)")
    public ResponseEntity<?> playWithCharacter(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("POST /character/play - User {}", userId);
        return ResponseEntity.ok(characterService.playWithCharacter(userId));
    }

    @GetMapping("/progression")
    @Operation(summary = "Get weekly progression status toward next character level")
    public ResponseEntity<?> getProgression(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("GET /character/progression - User {}", userId);
        return ResponseEntity.ok(gameProgressionService.getProgressionStatus(userId));
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized: missing JWT token"));
    }
}

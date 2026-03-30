package org.example.nbcheckinservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.dto.CharacterSelectionRequest;
import org.example.nbcheckinservice.entity.UserCharacter;
import org.example.nbcheckinservice.service.UserCharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
@Slf4j
public class CharacterController {

    private final UserCharacterService characterService;

    @GetMapping
    public ResponseEntity<CharacterResponse> getCharacter(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(characterService.getCharacterResponse(userId));
    }

    @PostMapping("/change-type")
    public ResponseEntity<CharacterResponse> changeCharacterType(
            @AuthenticationPrincipal Long userId,
            @RequestParam UserCharacter.CharacterType characterType
    ) {
        return ResponseEntity.ok(characterService.changeCharacterType(userId, characterType));
    }

    @PostMapping("/add-xp")
    public ResponseEntity<CharacterResponse> addXp(
            @AuthenticationPrincipal Long userId,
            @RequestParam int xp
    ) {
        return ResponseEntity.ok(characterService.addXp(userId, xp));
    }

    @PostMapping("/feed")
    public ResponseEntity<CharacterResponse> feedCharacter(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(characterService.feedCharacter(userId));
    }

    @PostMapping("/play")
    public ResponseEntity<CharacterResponse> playWithCharacter(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(characterService.playWithCharacter(userId));
    }

    @PostMapping("/select")
    public ResponseEntity<CharacterResponse> selectCharacter(
            @AuthenticationPrincipal Long userId,
            @Validated @RequestBody CharacterSelectionRequest selectionRequest
    ) {
        CharacterResponse response =
                characterService.selectInitialCharacter(userId, selectionRequest);

        return ResponseEntity.ok(response);
    }
}
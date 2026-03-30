package org.example.nbcheckinservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull; // или javax, в зависимости от версии

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSelectionRequest {

    @NotNull(message = "Character type is required")
    private String characterType;

    private String customName;
}
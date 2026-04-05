package org.example.ainote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.ainote.dto.validation.OnCreate;
import org.example.ainote.dto.validation.OnUpdate;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Note DTO
 */
@Data
@Schema(description = "Note DTO")
public class NoteDTO {

    @Schema(description = "Id")
    private Long id;

    @Schema(description = "Title")
    @NotNull(message = "Title cannot be null", groups = {OnCreate.class, OnUpdate.class})
    @Length(max = 500, message = "Title length cannot exceed 500 characters",
            groups = {OnCreate.class, OnUpdate.class})
    private String title;

    @Schema(description = "Content")
    private String content;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime created;
}
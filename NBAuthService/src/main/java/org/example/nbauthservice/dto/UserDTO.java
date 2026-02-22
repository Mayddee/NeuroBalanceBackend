package org.example.nbauthservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.nbauthservice.dto.validation.OnCreate;
import org.example.nbauthservice.dto.validation.OnUpdate;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description="User DTO")
public class UserDTO {

    @Schema(description = "Id")
    @NotNull(message = "Id can not be null",groups = OnUpdate.class)
    private Long id;

    @Schema(description = "Name")
    @NotNull(message = "Name can not be null", groups = OnCreate.class)
    @Length(max = 255, message = "Name length can not be smaller than 255 symbols.", groups = {OnCreate.class, OnUpdate.class})
    private String name;


    @Schema(description = "Email")
    @NotNull(message = "Email can not be null", groups = OnCreate.class)
    @Length(max = 255, message = "Email length can not be smaller than 255 symbols.", groups = {OnCreate.class, OnUpdate.class})
    private String email;


    @Schema(description = "Username")
    @NotNull(message = " Username can not be null", groups = {OnCreate.class, OnUpdate.class})
    @Length(max = 255, message = "Username length can not be smaller than 255 symbols.", groups = {OnCreate.class, OnUpdate.class})
    private String username;

    @Schema(description = "Password")
    @NotNull(message = " Password can not be null", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}

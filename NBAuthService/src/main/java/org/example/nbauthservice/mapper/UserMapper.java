package org.example.nbauthservice.mapper;

import org.example.nbauthservice.dto.UserDTO;
import org.example.nbauthservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);

    @Mapping(target = "roles", ignore = true)
    User toEntity(UserDTO userDTO);

}
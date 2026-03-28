package org.example.ainote.mapper;

import org.example.ainote.dto.UserDTO;
import org.example.ainote.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    @Mapping(target = "notes", expression = "java(new java.util.ArrayList<>())")
    User toEntity(UserDTO userDTO);
}
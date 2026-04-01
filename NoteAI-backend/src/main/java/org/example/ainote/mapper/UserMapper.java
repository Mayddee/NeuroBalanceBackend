package org.example.ainote.mapper;

import org.example.ainote.dto.UserDTO;
import org.example.ainote.entity.NoteUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(NoteUser user);

    @Mapping(target = "notes", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "userId", source = "id")
    NoteUser toEntity(UserDTO userDTO);
}

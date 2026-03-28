package org.example.ainote.mapper;

import org.example.ainote.dto.NoteDTO;
import org.example.ainote.entity.Note;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    NoteDTO toDto(Note note);

    Note toEntity(NoteDTO noteDTO);

    List<NoteDTO> toDto(List<Note> notes);
}
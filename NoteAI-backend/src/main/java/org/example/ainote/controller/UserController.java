package org.example.ainote.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.ainote.dto.NoteDTO;
import org.example.ainote.dto.UserDTO;
import org.example.ainote.dto.validation.OnCreate;
import org.example.ainote.dto.validation.OnUpdate;
import org.example.ainote.entity.Note;
import org.example.ainote.entity.User;
import org.example.ainote.mapper.NoteMapper;
import org.example.ainote.mapper.UserMapper;
import org.example.ainote.service.NoteService;
import org.example.ainote.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final NoteService noteService;
    private final UserMapper userMapper;
    private final NoteMapper noteMapper;

    @PutMapping
    public UserDTO update(@Validated(OnUpdate.class) @RequestBody UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        user = userService.update(user);
        return userMapper.toDto(user);
    }

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userMapper.toDto(userService.getById(id));
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/{id}/notes")
    public List<NoteDTO> getNotesByUserId(@PathVariable Long id) {
        List<Note> notes = noteService.getAllByUserId(id);
        return noteMapper.toDto(notes);
    }

    @PostMapping("/notes")
    public NoteDTO createNote(HttpServletRequest request,
                              @Validated(OnCreate.class) @RequestBody NoteDTO noteDTO) {
        Long userId = (Long) request.getAttribute("userId");
        Note note = noteMapper.toEntity(noteDTO);
        Note createdNote = noteService.createNoteForUser(userId, note.getTitle(), note.getContent());
        return noteMapper.toDto(createdNote);
    }
}
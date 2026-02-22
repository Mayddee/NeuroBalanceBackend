package org.example.nbauthservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.UserDTO;
import org.example.nbauthservice.dto.UserShortDto;
import org.example.nbauthservice.dto.validation.OnUpdate;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.mapper.UserMapper;
import org.example.nbauthservice.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    private final UserMapper userMapper;

    @PutMapping
    public UserDTO update(@Validated(OnUpdate.class) @RequestBody UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        user = userService.update(user);
        return userMapper.toDto(user);
    }

    @GetMapping("/search")
    public List<UserShortDto> search(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userMapper.toDto(userService.getById(id));
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        userService.delete(id);
    }

}

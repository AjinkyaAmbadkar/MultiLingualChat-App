package com.multilingual.chat.app.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multilingual.chat.app.dto.UserSummaryDto;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            log.info("Request came for creating User");
            User savedUser = userService.createUser(user);
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        List<UserSummaryDto> allUsers = userService.getAllUsers()
                .stream().map(UserSummaryDto::from).toList();
        return ResponseEntity.ok(allUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getId(@PathVariable long id) {
        try {
            return ResponseEntity.ok(UserSummaryDto.from(userService.getUserById(id)));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Returns the currently logged-in user's profile.
     * The email is read from the JWT Principal — no userId needed in the URL.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        try {
            return ResponseEntity.ok(UserSummaryDto.from(userService.getUserByEmail(principal.getName())));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Updates the preferred language for the currently logged-in user.
     *
     * PATCH /api/users/me/language
     * Body: { "preferredLanguage": "Spanish" }
     *
     * Uses PATCH (not PUT) because we're updating a single field, not replacing the whole resource.
     * The email comes from the JWT Principal — users can only update their own language.
     *
     * After this call, every subsequent message they RECEIVE will be translated into
     * the new language — MessageService reads preferredLanguage from DB on every message.
     */
    @PatchMapping("/me/language")
    public ResponseEntity<?> updateMyLanguage(@RequestBody Map<String, String> body, Principal principal) {
        try {
            String newLanguage = body.get("preferredLanguage");
            if (newLanguage == null || newLanguage.isBlank()) {
                return ResponseEntity.badRequest().body("preferredLanguage is required");
            }
            log.info("PATCH /api/users/me/language | user: {} → {}", principal.getName(), newLanguage);
            User updated = userService.updatePreferredLanguage(principal.getName(), newLanguage);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}

package com.multilingual.chat.app.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {

        log.info("Attempting to create user with email: {}", user.getemail());

        Optional<User> existingUser = userRepository.findByEmail(user.getemail());

        if (existingUser.isPresent()) {
            log.warn("User creation failed — email already exists: {}", user.getemail());
            throw new RuntimeException("User with this email is already present");
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully | userId: {} | email: {}", savedUser.getId(), savedUser.getemail());
        return savedUser;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.debug("Fetched {} users from DB", users.size());
        return users;
    }

    public User getUserById(long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for id: {}", id);
                    return new RuntimeException("User not found for id " + id);
                });
    }

}

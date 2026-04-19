package com.multilingual.chat.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {

        Optional<User> existingUser = userRepository.findByEmail(user.getemail());

        if (existingUser.isPresent()) {
            throw new RuntimeException("User with this email is already present");
        }

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found for id " + id));
    }

}

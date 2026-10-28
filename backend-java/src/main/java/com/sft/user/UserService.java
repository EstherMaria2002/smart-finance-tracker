package com.sft.user;

import com.sft.common.exception.ConflictException;
import com.sft.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public User create(String email, String passwordHash, String displayName) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName == null || displayName.isBlank() ? null : displayName.trim());
        return userRepository.save(user);
    }
}

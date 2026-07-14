package org.example.hive.service;

import org.example.hive.domain.User;
import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional(readOnly = true)
    public UserResponseDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDto updateMe(Long userId, UpdateMeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));

        boolean changingEmail = req.getEmail() != null && !req.getEmail().equals(user.getEmail());
        boolean changingPassword = req.getPassword() != null && !req.getPassword().isBlank();

        if (changingEmail || changingPassword) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                throw new UserException("Current password is required", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new UserException("Current password is incorrect", HttpStatus.BAD_REQUEST);
            }
        }

        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        if (changingEmail) {
            if (userRepository.existsByEmailAndIdNot(req.getEmail(), userId)) {
                throw new UserException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(req.getEmail());
        }
        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return UserMapper.toResponse(userRepository.save(user));
    }


    public Page<UserResponseDto> list(Long companyId, Pageable pageable) {
        throw new UserException("Not ready yet", HttpStatus.BAD_REQUEST);
    }

    public UserResponseDto getById(Long id, Long companyId) {
        throw new UserException("Not ready yet", HttpStatus.BAD_REQUEST);
    }

    public UserResponseDto create(CreateUserRequest req, Long companyId) {
        throw new UserException("Not ready yet", HttpStatus.BAD_REQUEST);
    }

    public UserResponseDto update(Long id, UpdateUserRequest req, Long companyId, Long currentUserId) {
        throw new UserException("Not ready yet", HttpStatus.BAD_REQUEST);
    }

    public void delete(Long id, Long companyId, Long currentUserId) {
        throw new UserException("Not ready yet", HttpStatus.BAD_REQUEST);
    }
}
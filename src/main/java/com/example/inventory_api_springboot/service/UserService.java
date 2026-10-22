package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.projection.UserDetailProjection;
import com.example.inventory_api_springboot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getReferenceById(UUID id) {
        return userRepository.getReferenceById(id);
    }

    public ResponseEntity<UserDetailProjection> getUserDetail(UUID userId) {
        UserDetailProjection userDetail = userRepository.getUserDetailById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User detail not found"));

        return ResponseEntity.ok(userDetail);
    }

    public ResponseEntity<String> blockUser(UUID userId) {
        userRepository.blockUser(userId);

        return ResponseEntity.ok()
            .body("User successfully blocked");
    }
}

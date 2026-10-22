package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSeeder {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private List<User> seederDataMapping() {
        List<User> listUser = List.of(
            User.builder()
                .email("superadmin@mail.com")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRoleEnum.USER_ADMIN)
                .build()
        );

        return listUser;
    }

    public void seedDatabase() {
        userRepository.saveAll(seederDataMapping());
    }

    public void deleteAll() {
        userRepository.deleteAll();
        userRepository.flush();
    }
}

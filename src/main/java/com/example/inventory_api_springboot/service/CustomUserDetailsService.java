package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.models.CustomUserDetails;
import com.example.inventory_api_springboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       User user = userRepository.findByEmail(email)
               .orElseThrow(() -> new UsernameNotFoundException("User not found !"));
       return new CustomUserDetails(user.getId(), user.getPassword());
    }
}

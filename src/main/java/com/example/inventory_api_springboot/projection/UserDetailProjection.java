package com.example.inventory_api_springboot.projection;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public interface UserDetailProjection {
    UUID getUserId();

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getName();

    String getPhoneNumber();
    String getAddress();
}

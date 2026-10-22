package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.PendingInventoryProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PendingInventoryProcessRepository extends JpaRepository<PendingInventoryProcess, UUID> { }

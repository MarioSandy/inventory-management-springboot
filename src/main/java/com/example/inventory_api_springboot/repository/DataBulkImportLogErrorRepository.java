package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.DataBulkImportLogError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataBulkImportLogErrorRepository extends JpaRepository<DataBulkImportLogError, UUID> {
}

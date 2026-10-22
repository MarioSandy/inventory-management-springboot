package com.example.inventory_api_springboot.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvalidExcelRow {
    private int rowNumber;
    private String error;
}

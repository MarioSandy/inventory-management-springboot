package com.example.inventory_api_springboot.repository.inventory;

import com.example.inventory_api_springboot.dto.query.request.InventoryStockOpnameDto;
import com.example.inventory_api_springboot.dto.query.response.InventoryStockOpnameExcelDto;
import com.example.inventory_api_springboot.dto.queue.InventoryStockOpnameExcelPayloadDto;
import com.example.inventory_api_springboot.entity.InventoryLog;

import java.util.List;
import java.util.UUID;

public interface InventoryNativeRepository {
    List<InventoryStockOpnameExcelDto> getInventoryForStockOpnameExcel(UUID cursor);

    int[][] inventoryStockOpname(
        List<InventoryStockOpnameDto> listInventoryStockOpname,
        UUID userId
    );

}

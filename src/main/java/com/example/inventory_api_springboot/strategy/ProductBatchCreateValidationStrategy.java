package com.example.inventory_api_springboot.strategy;

import com.example.inventory_api_springboot.dto.queue.InvalidExcelRow;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductBatchCreateValidationStrategy extends ExcelOrchestrationStrategy {

    @Override
    public String getUseCaseName() {
        return OutboxEventTypeEnum.CREATE_PRODUCT_BATCH.toString();
    }

    @Override
    protected List<InvalidExcelRow> validateSheet(Sheet sheet) {
        List<InvalidExcelRow> listInvalidRow = new ArrayList<InvalidExcelRow>();
        List<Map<String, Object>> listMasterData = new ArrayList<Map<String, Object>>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            Optional<Map<String, Object>> duplicatedMasterData = listMasterData.stream().filter(
                map ->
                    map.get("").toString().equals(row.getCell(2).getStringCellValue())
                    && map.get("").toString().equals(row.getCell(3).getStringCellValue())
                    && map.get("").toString().equals(row.getCell(5).getStringCellValue())
                    && map.get("").toString().equals(row.getCell(6).getStringCellValue())
            )
            .findFirst();

            if (duplicatedMasterData.isPresent()) {
                listInvalidRow.add(
                    new InvalidExcelRow(
                        row.getRowNum(),
                        "There are duplication data"
                    )
                );
            } else {
                listMasterData.add(
                    Map.of(
                        "category", row.getCell(2).getStringCellValue(),
                        "brand", row.getCell(3).getStringCellValue(),
                        "color", row.getCell(5).getStringCellValue(),
                        "price", row.getCell(6).getStringCellValue()
                    )
                );
            }
        }

        return listInvalidRow;
    }
}

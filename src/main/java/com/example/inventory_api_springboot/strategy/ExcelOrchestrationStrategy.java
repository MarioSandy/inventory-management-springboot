package com.example.inventory_api_springboot.strategy;

import com.example.inventory_api_springboot.dto.queue.InvalidExcelRow;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.util.List;
import java.util.Set;

public abstract class ExcelOrchestrationStrategy {
    public abstract String getUseCaseName();

    public final List<InvalidExcelRow> validateWorksheet(
        File file,
        int sheetIndex
    ) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            return validateSheet(sheet);
        }
    }

    protected abstract List<InvalidExcelRow> validateSheet(Sheet sheet);
}

package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.StringJoiner;

@Slf4j
@Component
public class ExcelParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "xlsx".equalsIgnoreCase(fileType) || "xls".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            StringBuilder sb = new StringBuilder();
            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append("# ").append(sheet.getSheetName()).append("\n\n");

                Row headerRow = sheet.getRow(0);
                String[] headers = null;
                if (headerRow != null) {
                    headers = new String[headerRow.getLastCellNum()];
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        headers[c] = cell != null ? formatter.formatCellValue(cell) : "";
                    }
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    StringJoiner joiner = new StringJoiner(", ");
                    boolean hasContent = false;
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        String value = cell != null ? formatter.formatCellValue(cell).trim() : "";
                        if (!value.isEmpty()) hasContent = true;

                        String header = (headers != null && c < headers.length) ? headers[c] : "col" + c;
                        if (!value.isEmpty()) {
                            joiner.add(header + ": " + value);
                        }
                    }
                    if (hasContent) {
                        sb.append(joiner).append("\n");
                    }
                }
            }

            return sb.toString();
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse Excel", e);
        }
    }
}

package com.fnb.report.util;

import com.fnb.report.dto.InventoryVarianceDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelExporter {

    public byte[] exportInventoryVariance(List<InventoryVarianceDto> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory Variance (TvA)");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            // Header Font
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            // Data Styles
            DataFormat dataFormat = workbook.createDataFormat();
            
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] columns = {"Nguyên liệu", "ĐVT", "Lý thuyết (Formula)", "Thực tế tiêu thụ", "Chênh lệch", "Giá trị thiệt hại (VNĐ)"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (InventoryVarianceDto item : data) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);
                
                Cell c0 = row.createCell(0);
                c0.setCellValue(item.getIngredientName());
                c0.setCellStyle(textStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(item.getUomName());
                c1.setCellStyle(textStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(item.getTheoreticalUsage() != null ? item.getTheoreticalUsage().doubleValue() : 0.0);
                c2.setCellStyle(numberStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(item.getActualUsage() != null ? item.getActualUsage().doubleValue() : 0.0);
                c3.setCellStyle(numberStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(item.getVariance() != null ? item.getVariance().doubleValue() : 0.0);
                c4.setCellStyle(numberStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(item.getVarianceValue() != null ? item.getVarianceValue().doubleValue() : 0.0);
                c5.setCellStyle(currencyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Give a little extra width for padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}

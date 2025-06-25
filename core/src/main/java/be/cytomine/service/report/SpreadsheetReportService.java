package be.cytomine.service.report;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;

@Service
@Slf4j
public class SpreadsheetReportService {

    /**
     * Write a spreadsheet report (xls)
     *
     * @param  dataArray
     * @return Spreadsheet byte array report encoded in base 64
     */
    public byte[] writeSpreadsheet(Object[][] dataArray) throws ServerException {
        log.info(String.format("Generating spread sheet with delimiter: '%s'", ";"));
        CSVFormat format = CSVFormat.EXCEL.withDelimiter(';');
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(output), format)){

            for (Object[] row : dataArray) {
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();

            log.info("Spread sheet file has been generated");
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate spread sheet. Error: %s".format(e.getMessage()));
            throw new ServerException(String.format("Cannot generate spread sheet with params: format=%s. Error: %s",format, e.getMessage()));
        }
    }


    /**
     * Write a spreadsheet report (xls)
     *
     * @param  dataArray
     * @return Spreadsheet byte array report encoded in base 64
     */
    public byte[] writeSpreadsheetXLS(Object[][] dataArray) throws ServerException {
        log.info(String.format("Generating spread sheet with delimiter: '%s'", ";"));
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Sheet1");
        int rowNum = 0;
        for (var row : dataArray) {
            var currentRow = sheet.createRow(rowNum++);
            int colNum = 0;
            for (var field : row) {
                var currentCell = currentRow.createCell(colNum++);
                if (field instanceof String) {
                    currentCell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    currentCell.setCellValue((Integer) field);
                } else if (field instanceof Double) {
                    currentCell.setCellValue((Double) field);
                } else if (field instanceof Boolean) {
                    currentCell.setCellValue((Boolean) field);
                } else if (field instanceof Long) {
                    currentCell.setCellValue((Long) field);
                } else if (field instanceof BigDecimal) {
                    currentCell.setCellValue(((BigDecimal) field).doubleValue());
                }
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            log.info("Spreadsheet file has been generated");
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate spreadsheet. Error: %s".format(e.getMessage()));
            throw new ServerException(String.format("Cannot generate spreadsheet. Error: %s", e.getMessage()));
        }

    }

}


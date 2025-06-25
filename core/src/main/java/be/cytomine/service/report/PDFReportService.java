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
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.datatable.DataTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class PDFReportService {

    static final int MARGIN = 20;
    static final float MAX_COLUMN_WIDTH = (float) 0.2;
    static final int TOP_POSITION = 20;
    static final int BOTTOM_POSITION = 30;
    static final int PAGINATION_TOP_POSITION = 20;
    static final int PAGINATION_BOTTOM_POSITION = 20;
    static final int FONT_SIZE = 12;

    private PDDocument document;
    private PDPage page;
    private PDRectangle pageSize;
    private float contentWidth;
    private float contentHeight;
    private float maxPercentWidth;
    private boolean documentIsUsed = false;
    /**
     * PDFWriter constructor
     */
    public PDFReportService() {
        initPDFWriterService();
    }

    /**
     * PDFWriter constructor
     *
     * @param  maxPercentWidth Maximum width of a column (in percent) <br>
     *                         - Set btw 0 and 1 to cap column width <br>
     *                         - Set to 1 to obtain all columns proportional
     *                         to their number of characters. <br>
     *                         - Set to 0 to obtain uniform column width.
     */
    public PDFReportService(float maxPercentWidth) {
        initPDFWriterService();
        this.maxPercentWidth = maxPercentWidth;
    }

    /**
     * Init PDFWriter variables
     */
    private void initPDFWriterService(){
        log.info("Initializing PDF document");
        document = new PDDocument();
        page = new PDPage();
        page.setMediaBox(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);

        pageSize = page.getMediaBox();
        contentWidth = pageSize.getWidth() - MARGIN * 2;
        contentHeight = pageSize.getHeight() - MARGIN;

        if (MAX_COLUMN_WIDTH <= (float) 0) {
            this.maxPercentWidth = (float) 0.1;
        } else {
            this.maxPercentWidth = MAX_COLUMN_WIDTH;
        }
    }

    /**
     * Write a PDF from given data array (all types allowed)
     *
     * @param  dataArray data to write on PDF
     * @param  hasPagination
     * @param  hasHeader
     *
     * @return PDF in a byte array encoded in base 64
     */
    public byte[] writePDF(Object[][] dataArray,
                           String title,
                           float[] columnWidth,
                           boolean hasPagination,
                           boolean hasHeader) throws ServerException {

        checkData(dataArray);
        if(documentIsUsed){
            initPDFWriterService();
        }
        drawDataTable(arrayToString(dataArray), columnWidth, hasHeader);

        if(title == null){
            log.error("Cannot generate pdf report with null title, expected type: String.");
            throw new ServerException("Cannot generate pdf report with null title, expected type: String.");
        }
        if(!title.isEmpty()) {
            writeMessage(document.getPage(0), TOP_POSITION, pageSize.getHeight() - BOTTOM_POSITION, title);
        }
        if(hasPagination) {
            setPagination();
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            document.close();

            // Convert inputStream to byte array
            ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            byte[] pdfByteArray = new byte[pdfInputStream.available()];
            pdfInputStream.read(pdfByteArray);

            log.info("PDF file has been generated");
            return pdfByteArray;
        } catch (IOException e) {
            log.error("Could not save and close pdf document. Error: ".format(e.getMessage()));
            throw new ServerException("Cannot generate pdf report with title=%s. Error: %s".format(title, e.getMessage()));
        }
    }

    /**
     * Verify that all rows have the same number of cells
     * Throw a Report Generation error if not
     *
     * @param  dataArray
     */
    private void checkData(Object[][] dataArray) throws ServerException {
        if(dataArray != null) {
            int nbOfCell = dataArray[0].length;
            for (Object[] row : dataArray) {
                if (row.length != nbOfCell) {
                    log.error("All data rows should have the same number of cells");
                    throw new ServerException("All data rows should have the same number of cells");
                }
            }
        }
    }

    /**
     * Transform given data array in a string
     * Value delimiter is space and row break is '\r\n'
     *
     * @param  dataArray
     *
     * @return String
     */
    private String arrayToString(Object[][] dataArray) throws ServerException {
        if(dataArray == null){
            log.error("Cannot generate pdf report with null data, expected type: Object[][].");
            throw new ServerException("Cannot generate pdf report with null data, expected type: Object[][].");
        }
        String stringArray = "";
        for(Object[] row : dataArray){
            stringArray += Arrays.toString(row).replace('[', ' ').replace(']', ' ') + "\r\n";
        }
        return stringArray;
    }

    /**
     * Draw the data table on PDF document
     *
     * @param  data
     *
     * @return
     */
    private void drawDataTable(String data, float[] columnWidth, boolean hasHeader) throws ServerException {
        log.info("Drawing data table");
        try{
            float yStart = contentHeight - MARGIN;
            float bottomMargin = MARGIN * 2;
            BaseTable baseTable = new BaseTable(yStart, contentHeight, bottomMargin, contentWidth, MARGIN, document, page, true, true);
            DataTable dataTable = new DataTable(baseTable, page);

            setDatatableColor(dataTable);
            dataTable.addCsvToTable(data, hasHeader, ',');

            List<Row<PDPage>> rows = dataTable.getTable().getRows();
            if(columnWidth != null){
                checkPercentArraySum(columnWidth);
                checkColumnWidthSize(columnWidth, rows.get(0).getCells().size());
                setColumnWidth(rows, columnWidth);
            }else{
                autoSetColumnWidth(rows);
            }

            baseTable.draw();
            documentIsUsed = true;
        }catch(IOException e){
            log.error("Failed to create or draw the data table: %s".format(e.getMessage()));
            throw new ServerException("Cannot draw data table: s".format(e.getMessage()));
        }
    }

    /**
     * Verify that the sum of a percent array is equal to 1
     * Throw a Report Generation error if not
     *
     * @param  percentArray
     */
    private void checkPercentArraySum(float[] percentArray) throws ServerException {
        float sum = 0;
        for(float width: percentArray){
            sum += width;
        }
        // Round sum to avoid incomprehensible result to 1.00001
        sum = (float) Math.round(sum * (float) 100) / (float) 100;
        if(sum != (float) 1.00){
            log.error(String.format("Sum of a percent array should be equal to 1, actual: %.2f", sum));
            throw new ServerException(String.format("The sum of a percent array should be equal to 1, actual: %.2f", sum));
        }
    }

    private void checkColumnWidthSize(float[] columnWidth, int nbOfCells) throws ServerException{
        if(columnWidth.length != nbOfCells){
            log.error("Column width length should be the same than your data rows length");
            throw new ServerException("Column width length should be the same than your data rows length");
        }
    }

    /**
     * Set the row colors of the given datable
     *
     * @param  dataTable
     *
     * @return
     */
    private void setDatatableColor(DataTable dataTable) {
        dataTable.getDataCellTemplateEven().setFillColor(Color.LIGHT_GRAY);
        dataTable.getDataCellTemplateOdd().setFillColor(Color.WHITE);
        dataTable.getHeaderCellTemplate().setFillColor(Color.BLACK);
        dataTable.getHeaderCellTemplate().setTextColor(Color.WHITE);
    }

    /**
     * Set column width of the given rows in proportion to the
     * number of characters contained in the cell.
     * (Only based on the first row of the table, excluding headers).
     * <br> <br>
     * If the maximum percentage of width that a cell can reach is
     * reached, then the difference between the desired size
     * (proportional to the number of characters)
     * and the maximum size is redistributed to the other cells.
     *
     * @param  rows
     * @return
     */
    private void autoSetColumnWidth(List<Row<PDPage>> rows){
        // Take second index (second row) if exists, to avoid possible headers.
        int secondIndex = 1;
        if(rows.size() == 1){
            secondIndex = 0;
        }
        List<Cell<PDPage>> cells = rows.get(secondIndex).getCells();
        int nbOfCellPerRow = cells.size();

        float totalTextLength = cells.stream().mapToInt(c -> c.getText().length()).sum();
        // For each cell, get the percent of text it has compared to total text
        double[] cellsSizePercentDoubles = cells
                .stream()
                .mapToDouble(c -> (c.getText().length() / totalTextLength))
                .toArray();

        float[] cellsSizePercent = new float[cellsSizePercentDoubles.length];
        for (int i = 0; i < cellsSizePercentDoubles.length; i++) {
            cellsSizePercent[i] = (float) cellsSizePercentDoubles[i];
        }

        int[] maxPercentIndexes = updateCellsSize(cellsSizePercent, nbOfCellPerRow);
        dispatchUnusedWidth(cellsSizePercent, nbOfCellPerRow, maxPercentIndexes);

        setColumnWidth(rows, cellsSizePercent);
    }

    /**
     * Cap 'cellsSizePercent' array values to 'maxPercentWidth' and dispatch the
     * percent excess to others values.
     *
     * @param  cellsSizePercent
     * @param  nbOfCellPerRow
     * @return An array of the list of the indexes that have reached the max percentage
     */
    private int[] updateCellsSize(float[] cellsSizePercent, float nbOfCellPerRow){
        int[] maxPercentIndexes = new int[(int) nbOfCellPerRow];
        Arrays.fill(maxPercentIndexes, -1);

        for(int i = 0; i < nbOfCellPerRow; i++){
            // If cell is too big
            if (cellsSizePercent[i] > maxPercentWidth){
                // mark index as max
                maxPercentIndexes[i] = i;
                // then calculate the excess percentage to dispatch
                float toDispatch = (cellsSizePercent[i] - maxPercentWidth) / nbOfCellPerRow;
                cellsSizePercent[i] = maxPercentWidth;
                // and dispatch it to other cells
                dispatch(cellsSizePercent, toDispatch, maxPercentIndexes);
            }
        }
        return maxPercentIndexes;
    }

    /**
     * Calculate the width unused btw 'cellsSizePercent' and PDF document width.
     * Then dispatch the difference in cells that have not reached max percent width value.
     *
     * @param  cellsSizePercent
     * @param  nbOfCellPerRow
     * @param  maxPercentIndexes
     * @return
     */
    private void dispatchUnusedWidth(float[] cellsSizePercent, float nbOfCellPerRow, int[] maxPercentIndexes){
        float actualWidth = 0;
        for(float percent : cellsSizePercent){
            actualWidth += percent * contentWidth;
        }
        float unusedWidth = (contentWidth - actualWidth) / contentWidth;
        dispatch(cellsSizePercent, unusedWidth / nbOfCellPerRow, maxPercentIndexes);
    }

    /**
     * Dispatch the given diff in cells that have not reached max percent width value.
     *
     * @param  cellsSizePercent
     * @param  diff
     * @param  maxPercentIndexes
     * @return
     */
    private void dispatch(float[] cellsSizePercent, float diff, int[] maxPercentIndexes){
        for(int i = 0; i < cellsSizePercent.length; i++ ){
            if (!Arrays.asList(maxPercentIndexes).contains(i)) cellsSizePercent[i] += diff;
        }
    }

    /**
     * Set column width according to column width percent values.
     *
     * @param  rows
     * @param  cellsSizePercent
     * @return
     */
    private void setColumnWidth(List<Row<PDPage>> rows, float[] cellsSizePercent) {
        for(Row<PDPage> row : rows) {
            for(int i = 0; i < row.getCells().size(); i++){
                Cell<PDPage> cell = row.getCells().get(i);
                cell.setWidth(cellsSizePercent[i] * contentWidth);
                cell.setAlign(HorizontalAlignment.CENTER);
            }
        }
    }

    /**
     * Write a given message at a given position on a given page.
     *
     * @param  page
     * @param  topPosition
     * @param  bottomPosition
     * @param  message
     * @return
     */
    private void writeMessage(PDPage page, float topPosition, float bottomPosition, String message) throws ServerException {
        try {
            PDPageContentStream contentStream  = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
            contentStream.beginText();
            contentStream.newLineAtOffset(topPosition, bottomPosition);
            contentStream.showText(message);
            contentStream.endText();
            contentStream.close();
        } catch (IOException e) {
            log.error("Failed to write message: %s".format(e.getMessage()));
            throw new ServerException("Cannot write message: %s".format(e.getMessage()));
        }
    }

    /**
     * Write the n° of page for each page of the PDF document.
     *
     * @return
     */
    private void setPagination() throws ServerException {
        log.info("Setting pagination");
        int nbOfPages = document.getNumberOfPages();
        for (int i = 0; i < nbOfPages; i++) {
            PDPage nthPage = document.getPage(i);
            writeMessage(nthPage, pageSize.getWidth() - PAGINATION_TOP_POSITION, PAGINATION_BOTTOM_POSITION, String.valueOf(i + 1));
        }
    }
}

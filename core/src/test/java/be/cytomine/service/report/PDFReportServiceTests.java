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

import be.cytomine.CytomineCoreApplication;
import be.cytomine.exceptions.ServerException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class PDFReportServiceTests {

    @Autowired
    PDFReportService pdfReportService;
    Object [][] validData = {
            {"ID", "Area (microns²)", "Perimeter (mm)", "X", "Y", "Image Filename", "View annotation picture", "View annotation on image"},
            {'2', (float) 23298.5267702416, 0.614768015861511, 736, true, (long) 6836067, "https://...", "https://..."},
    };
    Object [][] diffSizeOfRowsData = {
            {"ID", "Area (microns²)", "Perimeter (mm)"},
            {'2', (float) 23298.5267702416, 0.614768015861511, 736, true}
    };
    Object [][] emptyData = {{}};

    float[] customColumnWidth = {(float)0.1, (float)0.1, (float)0.1, (float)0.1, (float)0.1, (float)0.2, (float)0.15, (float)0.15};
    float[] invalidCustomColumnWidthSum = {(float)0.1, (float)0.1, (float)0.1, (float)0.1, (float)0.1, (float)0.2, (float)0.2, (float)0.2};
    float[] invalidSizeCustomColumnWidth = {(float)0.1, (float)0.1, (float)0.1, (float)0.3, (float)0.2, (float)0.2};

    @Test
    public void generate_pdf_with_valid_data_works() throws IOException {
        testData(validData, "title", null,true, true);
    }

    @Test
    public void generate_pdf_with_empty_data_fails() throws IOException {
        testData(emptyData, "title", null,true, true);
    }

    @Test
    public void generate_pdf_with_null_data_fails(){
        ServerException expectedError = new ServerException(
                "Cannot generate pdf report with null data, expected type: Object[][]."
        );
        ServerException error = assertThrows(ServerException.class, () -> {
            testData(null, "title", null,true, true);
        });
        assertEquals(expectedError.getMessage(), error.getMessage());
    }

    @Test
    public void generate_pdf_with_null_title_fails(){
        ServerException expectedError = new ServerException(
                "Cannot generate pdf report with null title, expected type: String."
        );
        ServerException error = assertThrows(ServerException.class, () -> {
            testData(validData, null, null,true, true);
        });
        assertEquals(expectedError.getMessage(), error.getMessage());
    }

    @Test
    public void generate_pdf_without_pagination_works() throws IOException {
        testData(validData, "title", null,false, true);
    }

    @Test
    public void generate_pdf_with_custom_column_width_works() throws IOException {
        testData(validData, "title", customColumnWidth,false, true);
    }

    @Test
    public void sum_of_custom_column_width_values_different_from_1_fails() {
        ServerException expectedError = new ServerException("The sum of a percent array should be equal to 1, actual: 1.10");
        ServerException error = assertThrows(ServerException.class, () -> {
            testData(validData, "title", invalidCustomColumnWidthSum,true, true);
        });
        assertEquals(expectedError.getMessage(), error.getMessage());
    }

    @Test
    public void custom_column_width_with_different_size_than_data_fails(){
        ServerException expectedError = new ServerException("Column width length should be the same than your data rows length");
        ServerException error = assertThrows(ServerException.class, () -> {
            testData(validData, "title", invalidSizeCustomColumnWidth,true, true);
        });
        assertEquals(expectedError.getMessage(), error.getMessage());
    }

    @Test
    public void generate_pdf_with_different_size_of_rows_fails() throws ServerException {
        ServerException expectedError = new ServerException("All data rows should have the same number of cells");
        ServerException error = assertThrows(ServerException.class, () -> {
            testData(diffSizeOfRowsData, "title", null,true, true);
        });
        assertEquals(expectedError.getMessage(), error.getMessage());
    }

    @Test
    public void generate_pdf_without_header_works() throws ServerException, IOException {
        testData(validData, "title", null,true, false);
    }

    private void testData(Object[][] dataArray,
                          String title,
                          float[] customColumnWidth,
                          boolean hasPagination,
                          boolean hasHeader) throws ServerException, IOException {
        byte[] pdfByteArray = pdfReportService.writePDF(dataArray, title, customColumnWidth, hasPagination, hasHeader);
        PDDocument.load(pdfByteArray);
    }
}

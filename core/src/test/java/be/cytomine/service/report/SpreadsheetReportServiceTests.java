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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class SpreadsheetReportServiceTests {

    @Autowired
    SpreadsheetReportService spreadsheetReportService;

    Object [][] validData = {
            {"ID", "Area (microns²)", "Perimeter (mm)", "X", "Y", "Image Filename", "View annotation picture", "View annotation on image"},
            {'2', (float) 23298.5267702416, 0.614768015861511, 736, true, (long) 6836067, "https://...", "https://...", "0.614768015861511", "736", "1978.375", "6836067"},
    };
    String validDataResult = "ID;Area (microns²);Perimeter (mm);X;Y;Image Filename;View annotation picture;View annotation on image\r\n2;23298.527;0.614768015861511;736;true;6836067;https://...;https://...;0.614768015861511;736;1978.375;6836067\r\n";

    Object [][] delimiterInData = {{"ID", null, "Perimeter ; (mm)"},};
    String delimiterInDataResult = "ID;;\"Perimeter ; (mm)\"\r\n";

    Object [][] nullData = {{"ID", null, "Perimeter (mm)"},};
    String nullCsvDataResult = "ID;;Perimeter (mm)\r\n";

    Object [][] emptyData = {{}};
    String emptyCsvDataResult = "\r\n";

    @Test
    public void generate_spreadsheets_with_valid_data_works() {
        assertEquals(createSpreadsheet(validData), validDataResult);
    }

    @Test
    public void generate_spreadsheets_with_delimiter_in_data_works() {
        assertEquals(createSpreadsheet(delimiterInData), delimiterInDataResult);
    }

    @Test
    public void null_data_return_empty_string() {
        assertEquals(createSpreadsheet(nullData), nullCsvDataResult);
    }

    @Test
    public void empty_data_array_return_empty_string() {
        assertEquals(createSpreadsheet(emptyData), emptyCsvDataResult);
    }

    private String createSpreadsheet(Object[][] data) {
        byte[] csvByteArray = spreadsheetReportService.writeSpreadsheet(data);
        return new String(csvByteArray);
    }
}

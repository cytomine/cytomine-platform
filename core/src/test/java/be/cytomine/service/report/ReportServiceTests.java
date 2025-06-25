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
import be.cytomine.service.utils.ReportFormatService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
@ExtendWith(MockitoExtension.class)
public class ReportServiceTests {

    ReportFormatService mockReportFormatService = mock(ReportFormatService.class);
    PDFReportService mockPdfWriterService = mock(PDFReportService.class);
    SpreadsheetReportService mockSpreadsheetWriterService = mock(SpreadsheetReportService.class);
    ReportService reportService = new ReportService(mockPdfWriterService, mockSpreadsheetWriterService, mockReportFormatService);


    List<JsonObject> jsonObjectData = new ArrayList<>();
    List<Map<String, Object>> dataMap =  List.of(
            Map.of("id", "Hello"),
            Map.of("id", "World")
    );
    public static List<ReportColumn> columns = new ArrayList<>(){{
        add(new ReportColumn("id", "ID", (float) 0.05));
    }};
    Set<String> terms = new HashSet<>(Arrays.asList("term1", "term2"));
    Set<String> users = new HashSet<>(Arrays.asList("user1", "user2"));
    byte[] returnedReport = {1};

    @Test
    public void generate_csv_report_with_connection_history() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheet(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateConnectionHistoryReport("projectName", "userName", jsonObjectData);
        verify(mockReportFormatService, times(1))
                .formatJsonObjectForReport(ReportService.CONNECTION_HISTORY_REPORT_COLUMNS, jsonObjectData);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheet(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_csv_report_with_image_consultation() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheet(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateImageConsultationReport("projectName", "userName", jsonObjectData);
        verify(mockReportFormatService, times(1))
                .formatJsonObjectForReport(ReportService.IMAGE_CONSULTATION_COLUMNS, jsonObjectData);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheet(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_pdf_report_with_annotations() throws ServerException {
        when(mockPdfWriterService.writePDF(any(),any(),any(),anyBoolean(),anyBoolean()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateAnnotationsReport("projectName", terms, users, dataMap, "pdf");
        verify(mockReportFormatService, times(1))
                .formatAnnotationsForReport(ReportService.ANNOTATION_REPORT_COLUMNS, dataMap);
        verify(mockPdfWriterService, times(1))
                .writePDF(any(),any(),any(),anyBoolean(),anyBoolean());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_csv_report_with_annotations() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheet(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateAnnotationsReport("projectName" ,terms, users, dataMap, "csv");
        verify(mockReportFormatService, times(1))
                .formatAnnotationsForReport(ReportService.ANNOTATION_REPORT_COLUMNS, dataMap);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheet(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_xls_report_with_annotations() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheetXLS(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateAnnotationsReport("projectName", terms, users, dataMap, "xls");
        verify(mockReportFormatService, times(1))
                .formatAnnotationsForReport(ReportService.ANNOTATION_REPORT_COLUMNS, dataMap);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheetXLS(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_pdf_report_with_users() throws ServerException {
        when(mockPdfWriterService.writePDF(any(),any(),any(),anyBoolean(),anyBoolean()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateUsersReport("projectName", dataMap, "pdf");
        verify(mockReportFormatService, times(1))
                .formatMapForReport(ReportService.USER_REPORT_COLUMNS, dataMap);
        verify(mockPdfWriterService, times(1))
                .writePDF(any(),any(),any(),anyBoolean(),anyBoolean());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_csv_report_with_users() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheet(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateUsersReport("projectName", dataMap, "csv");
        verify(mockReportFormatService, times(1))
                .formatMapForReport(ReportService.USER_REPORT_COLUMNS, dataMap);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheet(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void generate_xls_report_with_users() throws ServerException {
        when(mockSpreadsheetWriterService.writeSpreadsheetXLS(any()))
                .thenReturn(returnedReport);
        byte[] generatedReport = reportService.generateUsersReport("projectName", dataMap, "xls");
        verify(mockReportFormatService, times(1))
                .formatMapForReport(ReportService.USER_REPORT_COLUMNS, dataMap);
        verify(mockSpreadsheetWriterService, times(1))
                .writeSpreadsheetXLS(any());
        assertArrayEquals(returnedReport, generatedReport);
    }

    @Test
    public void invalid_report_format_return_server_error(){
        ServerException expectedError = new ServerException(
                "Failed to generate report, invalid format. Format should be one of these types: 'pdf', 'csv' or 'xls'."
        );
        ServerException error = assertThrows(ServerException.class, () -> {
            reportService.generateReport("title", new Object[][]{}, columns ,"invalidFormat");
        });
        assertEquals(expectedError.getMessage(),  error.getMessage());
    }
}

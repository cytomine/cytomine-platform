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
import be.cytomine.service.utils.ReportFormatService;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ReportService {

    public static final boolean HAS_PAGINATION = true;
    public static final boolean HAS_HEADER = true;
    public static final List<ReportColumn> ANNOTATION_REPORT_COLUMNS = new ArrayList<>(){{
        add(new ReportColumn("id", "Id", (float) 0.05));
        add(new ReportColumn("area", "Area (microns²)", (float) 0.10));
        add(new ReportColumn("perimeter", "Perimeter (mm)", (float) 0.10));
        add(new ReportColumn("X", "X", (float) 0.05));
        add(new ReportColumn("Y", "Y", (float) 0.05));
        add(new ReportColumn("image", "Image Id", (float) 0.07));
        add(new ReportColumn("filename", "Image Filename", (float) 0.10));
        add(new ReportColumn("user", "User", (float) 0.05));
        add(new ReportColumn("term", "Term", (float) 0.05));
        add(new ReportColumn("cropURL", "View annotation picture", (float) 0.19));
        add(new ReportColumn("imageURL", "View annotation on image", (float) 0.19));
    }};
    public static final List<ReportColumn> USER_REPORT_COLUMNS = new ArrayList<>(){{
        add(new ReportColumn("username", "Username", (float) 0.5));
        add(new ReportColumn("name", "Name", (float) 0.5));
    }};
    public static final List<ReportColumn> IMAGE_CONSULTATION_COLUMNS = new ArrayList<>(){{
        add(new ReportColumn("time", "Cumulated duration (ms)", (float) 0.10));
        add(new ReportColumn("first", "First consultation", (float) 0.15));
        add(new ReportColumn("last", "Last consultation", (float) 0.15));
        add(new ReportColumn("frequency", "Number of consultations", (float) 0.10));
        add(new ReportColumn("imageId", "Id of image", (float) 0.10));
        add(new ReportColumn("imageName", "Name", (float) 0.10));
        add(new ReportColumn("imageThumb", "Thumb", (float) 0.20));
        add(new ReportColumn("numberOfCreatedAnnotations", "Number of created annotations", (float) 0.10));
    }};
    public static final List<ReportColumn> CONNECTION_HISTORY_REPORT_COLUMNS = new ArrayList<>(){{
        add(new ReportColumn("created", "Date", (float) 0.20));
        add(new ReportColumn("time", "Duration (ms)", (float) 0.10));
        add(new ReportColumn("countViewedImages", "Number of viewed images", (float) 0.10));
        add(new ReportColumn("countCreatedAnnotations", "Number of created annotations", (float) 0.10));
        add(new ReportColumn("os", "Operating System", (float) 0.10));
        add(new ReportColumn("browser", "Browser", (float) 0.20));
        add(new ReportColumn("browserVersion", "Browser Version", (float) 0.20));
    }};
    private final PDFReportService pdfReportService;

    private final SpreadsheetReportService spreadsheetReportService;

    private final ReportFormatService reportFormatService;

    public byte[] generateConnectionHistoryReport(String projectName, String userName, List<JsonObject> data) {
        String title = getConnectionHistoryReportTitle(projectName, userName);
        return generateJsonObjectReport(title, data, CONNECTION_HISTORY_REPORT_COLUMNS);
    }

    public byte[] generateImageConsultationReport(String projectName, String userName, List<JsonObject> data) {
        String title = getImageConsultationReportTitle(projectName, userName);
        return generateJsonObjectReport(title, data, IMAGE_CONSULTATION_COLUMNS);
    }

    public byte[] generateUsersReport(String projectName, List<Map<String, Object>> data, String format) throws ServerException {
        Object[][] dataForReport = reportFormatService.formatMapForReport(USER_REPORT_COLUMNS, data);
        return generateReport(getUserReportTitle(projectName), dataForReport, USER_REPORT_COLUMNS, format);
    }

    public byte[] generateAnnotationsReport(String projectName, Set<String> terms, Set<String> users, List<Map<String, Object>> data, String format) throws ServerException {
        Object[][] dataForReport = reportFormatService.formatAnnotationsForReport(ANNOTATION_REPORT_COLUMNS, data);
        return generateReport(getAnnotationReportTitle(projectName, terms, users), dataForReport, ANNOTATION_REPORT_COLUMNS, format);
    }

    private byte[] generateJsonObjectReport(String title, List<JsonObject> data, List<ReportColumn> columns){
        Object[][] dataForReport = reportFormatService.formatJsonObjectForReport(columns, data);
        return generateReport(title, dataForReport, columns, "csv");
    }

    public byte[] generateReport(String title, Object[][] data, List<ReportColumn> columns, String format) throws ServerException {
        float[] columnWidth = reportFormatService.getColumnWidth(columns);
        switch (format){
            case "csv":
                return spreadsheetReportService.writeSpreadsheet(data);
            case "xls":
                return spreadsheetReportService.writeSpreadsheetXLS(data);
            case "pdf":
                return pdfReportService.writePDF(data, title, columnWidth, HAS_PAGINATION, HAS_HEADER);
            default :
                log.error("Format should be one of these types: 'pdf', 'csv' or 'xls'.");
                throw new ServerException("Failed to generate report, invalid format. Format should be one of these types: 'pdf', 'csv' or 'xls'.");
        }
    }

    private String getConnectionHistoryReportTitle(String projectName, String userName){
        return "Connections of user "+ userName + " to project " + projectName;
    }

    private String getImageConsultationReportTitle(String projectName, String userName){
        return "Consultations of images into project "+ projectName + " by user " + userName;
    }

    private String getAnnotationReportTitle(String projectName, Set<String> terms, Set<String> users) {
        return "Annotations in " + projectName + " created by " + String.join(" or ", users) + " and associated with " + String.join(" or ", terms) + " @ " + DateUtils.getLocaleDate(new Date());
    }

    private String getUserReportTitle(String projectName) {
        return "User in " + projectName + " created @ " + DateUtils.getLocaleDate(new Date());
    }

    public String getConnectionHistoryReportFileName(String format, Long projectId, Long userId){
        return "user_" + userId + "_connections_project_" + projectId + "_" + DateUtils.getSimpleFormatLocaleDate(new Date()) + "." + format;
    }

    public String getImageConsultationReportFileName(String format, Long projectId, Long userId){
        return "image_consultations_of_user_" + userId + "_project_" + projectId + "_" + DateUtils.getSimpleFormatLocaleDate(new Date()) + "." + format;
    }

    public String getAnnotationReportFileName(String format, Long projectId){
        return DateUtils.getSimpleFormatLocaleDate(new Date()) + "_annotations_project" + projectId + "." + format;
    }

    public String getUsersReportFileName(String format, Long projectId){
        return DateUtils.getSimpleFormatLocaleDate(new Date()) + "_users_project" + projectId + "." + format;
    }

}

package be.cytomine.service.utils;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.security.User;
import be.cytomine.dto.image.Point;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.UserService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ReportFormatServiceTests {

    private Object[][] expectedDataObject;

    @Autowired
    ReportFormatService reportFormatService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    UserService userService;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    ProjectService projectService;

    @Autowired
    TermService termService;

    @Test
    public void connection_history_to_report_format() {
        Object[][] dataObject = reportFormatService.formatJsonObjectForReport(
                ReportService.CONNECTION_HISTORY_REPORT_COLUMNS,
                buildUserConnectionHistory(true));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_connection_history_to_report_format() {
        Object[][] dataObject = reportFormatService.formatJsonObjectForReport(
                ReportService.CONNECTION_HISTORY_REPORT_COLUMNS,
                buildUserConnectionHistory(false));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void image_consultation_to_report_format() {
        Object[][] dataObject = reportFormatService.formatJsonObjectForReport(
                ReportService.IMAGE_CONSULTATION_COLUMNS,
                buildUserImageConsultation(true));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_image_consultation_to_report_format() {
        Object[][] dataObject = reportFormatService.formatJsonObjectForReport(
                ReportService.IMAGE_CONSULTATION_COLUMNS,
                buildUserImageConsultation(false));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatAnnotationsForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(true));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatAnnotationsForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(false));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void reviewed_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatAnnotationsForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(true));
        assertArrayEquals(expectedDataObject, dataObject);
    }
    @Test
    public void incomplete_reviewed_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatAnnotationsForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(false));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void users_to_report_format() {
        Object[][] dataObject = reportFormatService.formatMapForReport(
                ReportService.USER_REPORT_COLUMNS,
                buildUsers(true));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_users_to_report_format() {
        Object[][] dataObject = reportFormatService.formatMapForReport(
                ReportService.USER_REPORT_COLUMNS,
                buildUsers(false));
        assertArrayEquals(expectedDataObject, dataObject);
    }

    private List<JsonObject> buildUserConnectionHistory(boolean isComplete){
        Date date = new Date();
        expectedDataObject = new Object[][]{
                {"Date", "Duration (ms)", "Number of viewed images", "Number of created annotations", "Operating System", "Browser", "Browser Version"},
                {date, (long)5878, "10", "5", "Linux for life", "Firefox", "97.0.568"},
        };
        JsonObject userConnectionHistory = new JsonObject(Map.of(
                "created", date.getTime(),
                "time", 5878,
                "countViewedImages", 10,
                "countCreatedAnnotations", 5,
                "os", "Linux for life",
                "browser", "Firefox",
                "browserVersion", "97.0.568"
        ));
        if(!isComplete){
            expectedDataObject[1][5] = "";
            userConnectionHistory.remove("browser");
        }
        return new ArrayList<>(List.of(userConnectionHistory));
    }

    private List<JsonObject> buildUserImageConsultation(boolean isComplete){
        expectedDataObject = new Object[][]{
                {"Cumulated duration (ms)", "First consultation", "Last consultation", "Number of consultations", "Id of image", "Name", "Thumb", "Number of created annotations"},
                {(long)200, "Wed Mar 30 07:33:31 UTC 2022", "Wed Mar 31 07:33:31 UTC 2022", "5", "25454", "Beautiful image", "http://thumbURL", "2"},
        };
        JsonObject imageConsultation = new JsonObject(Map.of(
                "time", "200",
                "first", "Wed Mar 30 07:33:31 UTC 2022",
                "last", "Wed Mar 31 07:33:31 UTC 2022",
                "frequency", 5,
                "image", 25454,
                "imageName", "Beautiful image",
                "imageThumb", "http://thumbURL",
                "countCreatedAnnotations", "2"
        ));
        if(!isComplete){
            expectedDataObject[1][6] = "";
            imageConsultation.remove("imageThumb");
        }
        return new ArrayList<>(List.of(imageConsultation));
    }

    private List<Map<String,Object>> buildAnnotations(boolean isComplete){
        Term term1 = builder.given_a_term();
        Term term2 = builder.given_a_term();
        Point point = new Point(2545454.231212, 2545454.23111);
        expectedDataObject = new Object[][]{
                {"Id", "Area (microns²)", "Perimeter (mm)", "X", "Y", "Image Id", "Image Filename", "User", "Term", "View annotation picture", "View annotation on image"},
                {
                    "2",
                    "2545454.23",
                    "2545.23",
                    "2545454.23",
                    "2545454.23",
                    "1234567",
                    "Beautiful image",
                    "Paul",
                    termService.find(term1.getId()).get().getName() + "- " + termService.find(term2.getId()).get().getName(),
                    "http://cropURL",
                    "http://imageURL"
                },
        };
        Map<String,Object> annotations = new HashMap<>(Map.of(
                "id", "2",
                "image", "1234567",
                "instanceFilename", "Beautiful image",
                "area", 2545454.23,
                "perimeter", 2545.23,
                "creator", "Paul",
                "centroid", point,
                "term", term1.getId() + "," + term2.getId(),
                "cropURL", "http://cropURL",
                "imageURL", "http://imageURL"
        ));
        if(!isComplete){
            expectedDataObject[1][10] = "";
            annotations.remove("imageURL");
        }
        return new ArrayList<>(List.of(annotations));
    }

    private List<Map<String,Object>> buildUsers(boolean isComplete){
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();
        expectedDataObject = new Object[][]{
                {"Username", "Name"},
                {user1.getUsername(), user1.getName()},
                {user2.getUsername(), user2.getName()},
        };
        if(!isComplete){
            expectedDataObject[1][1] = "";
            return new ArrayList<>(List.of(
                    Map.of("username", user1.getUsername()),
                    Map.of("username", user2.getUsername(), "name", user2.getName())));
        }else{
            return new ArrayList<>(List.of(
                    Map.of("username", user1.getUsername(), "name", user1.getName()),
                    Map.of("username", user2.getUsername(), "name", user2.getName())));
        }
    }
}

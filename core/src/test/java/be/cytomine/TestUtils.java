package be.cytomine;

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

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.test.web.servlet.MvcResult;

import java.io.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestUtils {

    public static String getResourceFileAsString(String fileName) {
        InputStream is = getResourceFileAsInputStream(fileName);
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return (String) reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } else {
            throw new RuntimeException("resource not found");
        }
    }

    public static InputStream getResourceFileAsInputStream(String fileName) {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public static void checkSpreadsheetAnnotationResult(String delimiter, MvcResult result, AnnotationDomain annotationDomain, Project project, ImageInstance imageInstance, User user, Term term, String cropPath, String serverUrl) throws UnsupportedEncodingException {
        String[] rows = result.getResponse().getContentAsString().split("\n");
        String[] userAnnotationResult = rows[1].split(delimiter);
        assertThat(userAnnotationResult[0]).isEqualTo(annotationDomain.getId().toString());
        assertThat(userAnnotationResult[1]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getArea()));
        assertThat(userAnnotationResult[2]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getPerimeter()));
        assertThat(userAnnotationResult[3]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getX()));
        assertThat(userAnnotationResult[4]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getY()));
        assertThat(userAnnotationResult[5]).isEqualTo(imageInstance.getId().toString());
        assertThat(userAnnotationResult[6]).isEqualTo(imageInstance.getBlindInstanceFilename());
        assertThat(userAnnotationResult[7]).isEqualTo(user.getUsername());
        assertThat(userAnnotationResult[8]).isEqualTo(term.getName());
        assertThat(userAnnotationResult[9]).isEqualTo(serverUrl + "/api/" + cropPath + "/" + annotationDomain.getId() + "/crop.png");
        assertThat(userAnnotationResult[10].replace("\r", "")).isEqualTo(serverUrl + "/#/project/" + project.getId() + "/image/" + imageInstance.getId() + "/annotation/" + annotationDomain.getId());
    }

    public static void checkSpreadsheetXLSAnnotationResult(MvcResult result, AnnotationDomain annotationDomain, Project project, ImageInstance imageInstance, User user, Term term, String cropPath, String serverUrl) throws IOException {
        byte[] spreadsheetData = result.getResponse().getContentAsByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(spreadsheetData);
        Workbook workbook = null;

        workbook = new HSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        Row row = sheet.getRow(1); // Assuming the data starts from the second row
        Cell[] cells = new Cell[row.getLastCellNum()];
        for (int i = 0; i < row.getLastCellNum(); i++) {
            cells[i] = row.getCell(i);
        }

        assertThat((long)cells[0].getNumericCellValue()).isEqualTo(annotationDomain.getId());
        assertThat(cells[1].getStringCellValue()).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getArea()));
        assertThat(cells[2].getStringCellValue()).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getPerimeter()));
        assertThat(cells[3].getStringCellValue()).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getX()));
        assertThat(cells[4].getStringCellValue()).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getY()));
        assertThat((long)cells[5].getNumericCellValue()).isEqualTo(imageInstance.getId());
        assertThat(cells[6].getStringCellValue()).isEqualTo(imageInstance.getBlindInstanceFilename());
        assertThat(cells[7].getStringCellValue()).isEqualTo(user.getUsername());
        assertThat(cells[8].getStringCellValue()).isEqualTo(term.getName());
        assertThat(cells[9].getStringCellValue()).isEqualTo(serverUrl + "/api/" + cropPath + "/" + annotationDomain.getId() + "/crop.png");
        assertThat(cells[10].getStringCellValue().replace("\r", "")).isEqualTo(serverUrl + "/#/project/" + project.getId() + "/image/" + imageInstance.getId() + "/annotation/" + annotationDomain.getId());

        workbook.close();

    }
}

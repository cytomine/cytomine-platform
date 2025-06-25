package be.cytomine.domain;

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
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.repository.image.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class UploadedFileDomainTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    UploadedFileRepository uploadedFileRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void support_long_array_read() {
        for (UploadedFile uploadedFile : uploadedFileRepository.findAll()) {
            System.out.print(uploadedFile.getId() + " => ");
            if (uploadedFile.getProjects() != null) {
                Arrays.stream(uploadedFile.getProjects()).forEach(System.out::println);
            }
            System.out.println();
        }
    }


    @Test
    void support_long_array_persistence() {
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();
        uploadedFile.setProjects(new Long[]{1L,2L,3L});
        uploadedFile = uploadedFileRepository.save(uploadedFile);
        entityManager.flush();
        Long id = uploadedFile.getId();
        entityManager.refresh(uploadedFile);
        System.out.println("id = " + id);
        uploadedFile = entityManager.find(UploadedFile.class, id);
        assertThat(uploadedFile).isNotNull();
        assertThat(uploadedFile.getProjects()).isNotNull();
        assertThat(uploadedFile.getProjects()).contains(1L, 2L, 3L);
    }
}

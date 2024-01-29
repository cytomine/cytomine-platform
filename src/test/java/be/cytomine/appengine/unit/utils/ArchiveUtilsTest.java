package be.cytomine.appengine.unit.utils;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.utils.ArchiveUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

public class ArchiveUtilsTest {


    @Test
    public void readArchiveTest_testDefaultImageLocation() throws IOException, ValidationException, BundleArchiveException {
        ClassPathResource resource = new ClassPathResource("/artifacts/test_default_image_location_task.zip");

        MockMultipartFile testAppBundle = new MockMultipartFile("test_default_image_location_task.zip", resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        Assertions.assertNotNull(test);
        Assertions.assertNotNull(test.getDockerImage());
        Assertions.assertNotNull(test.getDescriptorFile());
        Assertions.assertNotNull(test.getDescriptorFileAsJson());
    }

    @Test
    public void readArchiveTest_testCustomImageLocation() throws IOException, ValidationException, BundleArchiveException {
        ClassPathResource resource = new ClassPathResource("/artifacts/test_custom_image_location_task.zip");

        MockMultipartFile testAppBundle = new MockMultipartFile("test_custom_image_location_task.zip", resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        Assertions.assertNotNull(test);
        Assertions.assertNotNull(test.getDockerImage());
        Assertions.assertNotNull(test.getDescriptorFile());
        Assertions.assertNotNull(test.getDescriptorFileAsJson());


    }

    @Test
    public void readArchiveTest_testBundleArchiveTypeDetection() throws IOException, ValidationException, BundleArchiveException {
        ClassPathResource resource = new ClassPathResource("/artifacts/test_wrong_archive_format_task.7z");

        MockMultipartFile testAppBundle = new MockMultipartFile("test_wrong_archive_format_task.7z", resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        BundleArchiveException bae = Assertions.assertThrows(BundleArchiveException.class, () -> {
            UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        });
        Assertions.assertTrue(bae.getError().getMessage().equalsIgnoreCase("unknown task bundle archive format"));
    }
}

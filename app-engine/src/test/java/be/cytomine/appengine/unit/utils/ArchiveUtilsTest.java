package be.cytomine.appengine.unit.utils;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.ValidationException;
import be.cytomine.appengine.utils.ArchiveUtils;
import be.cytomine.appengine.utils.TestTaskBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

public class ArchiveUtilsTest {


    @Test
    public void readArchiveTest_testDefaultImageLocation() throws IOException, ValidationException, BundleArchiveException {
        String bundleFilename = "test_default_image_location_task.zip";
        ClassPathResource resource = TestTaskBuilder.buildByBundleFilename(bundleFilename);

        MockMultipartFile testAppBundle = new MockMultipartFile(bundleFilename, resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        Assertions.assertNotNull(test);
        Assertions.assertNotNull(test.getDockerImage());
        Assertions.assertNotNull(test.getDescriptorFile());
        Assertions.assertNotNull(test.getDescriptorFileAsJson());
    }

    @Test
    public void readArchiveTest_testCustomImageLocation() throws IOException, ValidationException, BundleArchiveException {
        String bundleFilename = "test_custom_image_location_task.zip";
        ClassPathResource resource = TestTaskBuilder.buildByBundleFilename(bundleFilename);

        MockMultipartFile testAppBundle = new MockMultipartFile(bundleFilename, resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        Assertions.assertNotNull(test);
        Assertions.assertNotNull(test.getDockerImage());
        Assertions.assertNotNull(test.getDescriptorFile());
        Assertions.assertNotNull(test.getDescriptorFileAsJson());


    }

    @Test
    public void readArchiveTest_testBundleArchiveTypeDetection() throws IOException, ValidationException, BundleArchiveException {
        String bundleFilename = "test_wrong_archive_format_task.7z";
        ClassPathResource resource = TestTaskBuilder.buildByBundleFilename(bundleFilename);

        MockMultipartFile testAppBundle = new MockMultipartFile(bundleFilename, resource.getInputStream());
        ArchiveUtils archiveUtils = new ArchiveUtils();
        BundleArchiveException bae = Assertions.assertThrows(BundleArchiveException.class, () -> {
            UploadTaskArchive test = archiveUtils.readArchive(testAppBundle);

        });
        Assertions.assertTrue(bae.getError().getMessage().equalsIgnoreCase("unknown task bundle archive format"));
    }
}

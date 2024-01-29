package be.cytomine.appengine.utils;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.BundleArchiveException;
import be.cytomine.appengine.exceptions.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.zip.ZipEntry;

@Component
public class ArchiveUtils {

    static Logger logger = LoggerFactory.getLogger(ArchiveUtils.class);

    public UploadTaskArchive readArchive(MultipartFile archiveMultiplePartFile) throws ValidationException, BundleArchiveException {
        if (isZip(archiveMultiplePartFile)) return readZipArchive(archiveMultiplePartFile);
        // add other formats here

        AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_BUNDLE_ARCHIVE_FORAMT);
        throw new BundleArchiveException(error);
    }

    private boolean isZip(MultipartFile archiveMultiplePartFile) throws BundleArchiveException {
        Tika tika = new Tika();
        try {
            String type = tika.detect(archiveMultiplePartFile.getInputStream());
            logger.info("ArchiveUtils : archive detected " + type);

            return type.equalsIgnoreCase("application/zip");
        } catch (IOException e) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_IMAGE_ARCHIVE_FORMAT);
            throw new BundleArchiveException(error);
        }

    }

    public UploadTaskArchive readZipArchive(MultipartFile archive) throws BundleArchiveException, ValidationException {
        byte[] descriptorData = getDescriptorFileFromZip(archive);
        byte[] imageData;
        String customImageName = getCustomImageName(descriptorData);
        if (customImageName == null) imageData = getDockerImageFromZip(archive);
        else imageData = getDockerImageFromZip(archive, customImageName);
        return new UploadTaskArchive(descriptorData, imageData);
    }

    private String getCustomImageName(byte[] descriptorData) {
        try {
            return convertFromYmlDataToJson(descriptorData).get("configuration").get("image").get("file").textValue();
        } catch (Exception e) {
            logger.info("Buindle/Archive processing failure : no image location is configured in descriptor.yml, will fallback to default image location in root");
        }
        return null;
    }

    private JsonNode convertFromYmlDataToJson(byte[] descriptorFile) throws BundleArchiveException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode map;
        try {
            map = mapper.readTree(descriptorFile);
        } catch (IOException e) {
            logger.info("UploadTask : failed to convert descriptor.yml to json [" + e.getMessage() + "]");
            throw new BundleArchiveException(e);
        }
        return map;
    }

    private byte[] getDescriptorFileFromZip(MultipartFile archive) throws BundleArchiveException, ValidationException {

        try {
            byte[] descriptorData;
            try (ZipArchiveInputStream multiPartFileZipInputStream = new ZipArchiveInputStream(archive.getInputStream())) {
                ZipEntry ze;
                boolean descriptorNotFound = true;
                while ((ze = multiPartFileZipInputStream.getNextZipEntry()) != null) {
                    String name = ze.getName();
                    if (name.equalsIgnoreCase("descriptor.yml")) {
                        descriptorNotFound = false;
                        descriptorData = multiPartFileZipInputStream.readNBytes((int) ze.getSize());
                        return descriptorData;
                    }
                }
                if (descriptorNotFound) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DESCRIPTOR_NOT_IN_DEFAULT_LOCATION);
                    throw new ValidationException(error);
                }
            }
        } catch (IOException e) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DESCRIPTOR_EXTRACTION_FAILED);
            throw new BundleArchiveException(error);
        }
        return null;
    }

    private byte[] getDockerImageFromZip(MultipartFile archive) throws ValidationException, BundleArchiveException {
        return getDockerImageFromZip(archive, "image.tar");
    }

    private byte[] getDockerImageFromZip(MultipartFile archive, String customImageName) throws BundleArchiveException, ValidationException {
        try {
            byte[] imageData = null;
            try (ZipArchiveInputStream multiPartFileZipInputStream = new ZipArchiveInputStream(archive.getInputStream())) {
                if (customImageName != null)
                    logger.info("ArchiveUtils : reading image from custom location [" + customImageName + "] ...");
                if (customImageName.startsWith("/")) customImageName = customImageName.replaceFirst("/", "");

                ZipEntry ze;
                while ((ze = multiPartFileZipInputStream.getNextZipEntry()) != null) {
                    if (ze.getName().equalsIgnoreCase(customImageName)) {
                        imageData = multiPartFileZipInputStream.readNBytes((int) ze.getSize());
                        logger.info("ArchiveUtils : image is read");

                        return imageData;
                    }
                }

                if (imageData == null) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DOCKER_IMAGE_TAR_NOT_FOUND);
                    throw new ValidationException(error);
                }

            }
        } catch (IOException e) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_DOCKER_IMAGE_EXTRACTION_FAILED);
            throw new BundleArchiveException(error);
        }

        return null;
    }

}



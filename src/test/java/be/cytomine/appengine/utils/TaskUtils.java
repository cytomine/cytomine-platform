package be.cytomine.appengine.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.ParameterType;
import org.springframework.core.io.ClassPathResource;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.file.FileType;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.states.TaskRunState;

public class TaskUtils {
    public static UploadTaskArchive createTestUploadTaskArchive() throws IOException {
        File descriptorFile = new ClassPathResource("artifacts/descriptor.yml").getFile();

        // Create a copy because it will be deleted after the upload process
        Path original = new ClassPathResource("artifacts/image.tar").getFile().toPath();
        Path copy = Path.of("src/test/resources/artifacts/docker-test-image.tar");
        Files.copy(original, copy, StandardCopyOption.REPLACE_EXISTING);
        File dockerImage = copy.toFile();
        dockerImage.deleteOnExit();

        return new UploadTaskArchive(descriptorFile, dockerImage);
    }

    public static Author createTestAuthor() {
        Author author = new Author();
        author.setFirstName("Cytomine");
        author.setLastName("ULiege");
        author.setOrganization("University of Liege");
        author.setEmail("cytomine@uliege.be");
        author.setContact(true);

        return author;
    }

    public static Parameter createTestInput(String name, boolean binaryType) {
        Type type = binaryType ? new FileType() : new IntegerType();
        type.setCharset("UTF-8");

        Parameter input = new Parameter();
        input.setName(name);
        input.setDisplayName("Input");
        input.setDescription("Input description");
        input.setOptional(false);
        input.setType(type);
        input.setParameterType(ParameterType.INPUT);

        return input;
    }

    public static Parameter createTestOutput(String name, boolean binaryType) {
        Type type = binaryType ? new FileType() : new IntegerType();
        type.setCharset("UTF-8");

        Parameter output = new Parameter();
        output.setName(name);
        output.setDisplayName("Output");
        output.setDescription("output description");
        output.setOptional(false);
        output.setType(type);
        output.setParameterType(ParameterType.OUTPUT);

        return output;
    }

    public static Task createTestTask(boolean binaryType) {
        Task task = new Task();
        task.setIdentifier(UUID.randomUUID());
        task.setNamespace("namespace");
        task.setVersion("version");
        task.setStorageReference("storageReference");
        task.setDescription("Test Task Description");
        task.setAuthors(Set.of(createTestAuthor()));
        task.setParameters(Set.of(createTestInput("name", binaryType), createTestOutput("out", binaryType)));

        return task;
    }

    public static Task createTestTaskWithMultipleIO() {
        Task task = new Task();
        task.setIdentifier(UUID.randomUUID());
        task.setNamespace("namespace");
        task.setVersion("version");
        task.setStorageReference("storageReference");
        task.setDescription("Test Task Description");
        task.setAuthors(Set.of(createTestAuthor()));
        task.setParameters(Set.of(createTestInput("name 1", false),
            createTestInput("name 2", false),
            createTestOutput("out 1", false),
            createTestOutput("out 2", false)));

        return task;
    }

    public static Run createTestRun(boolean binaryType) {
        return new Run(
            UUID.randomUUID(),
            TaskRunState.CREATED,
            createTestTask(binaryType),
            UUID.randomUUID().toString()
        );
    }

    public static StorageData createTestStorageData(String parameterName, String storageId) throws IOException {
        File data = File.createTempFile("data", null);
        data.deleteOnExit();

        return new StorageData(new StorageDataEntry(data, parameterName, storageId, StorageDataType.FILE));
    }

    public static byte[] createFakeOutputsZip(String... names) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (String name : names) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write("42".getBytes());
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        }
    }
}

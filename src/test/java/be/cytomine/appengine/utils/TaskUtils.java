package be.cytomine.appengine.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;

import be.cytomine.appengine.dto.inputs.task.UploadTaskArchive;
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.models.task.integer.IntegerType;

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

    public static Input createTestInput() {
        Input input = new Input();
        input.setName("input");
        input.setDisplayName("Input");
        input.setDescription("Input description");
        input.setOptional(false);
        input.setType(new IntegerType());

        return input;
    }

    public static Task createTestTask() {
        Task task = new Task();
        task.setIdentifier(UUID.randomUUID());
        task.setNamespace("namespace");
        task.setVersion("version");
        task.setStorageReference("storageReference");
        task.setDescription("Test Task Description");
        task.setAuthors(Set.of(createTestAuthor()));
        task.setInputs(Set.of(createTestInput()));

        return task;
    }
}

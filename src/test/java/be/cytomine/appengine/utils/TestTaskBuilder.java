package be.cytomine.appengine.utils;


import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.models.task.Author;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.IntegerType;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.Task;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;


public class TestTaskBuilder {
  public static Task buildHardcodedAddInteger(UUID taskUUID) {
    String storageIdentifier = "task-" + taskUUID + "-def";
    String imageRegistryCompliantName = "com/cytomine/dummy/arithmetic/integer/addition:1.0.0";
    TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskUUID, storageIdentifier, imageRegistryCompliantName);

    // store two tasks in the database
    Task task = new Task();
    task.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
    task.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
    task.setName("Integers addition");
    task.setNameShort("add_integers");
    task.setDescriptorFile("com.cytomine.dummy.arithmetic.integer.addition");
    task.setNamespace("com.cytomine.dummy.arithmetic.integer.addition");
    task.setVersion("1.0.0");
    task.setDescription("");
    task.setInputFolder("/inputs");
    task.setOutputFolder("/outputs");
    // add authors
    Set<Author> authors = new HashSet<>();
    Author author = new Author();
    author.setFirstName("Romain");
    author.setLastName("Mormont");
    author.setOrganization("Cytomine Corporation");
    author.setEmail("romain.mormont@cytomine.com");
    author.setContact(true);
    authors.add(author);
    task.setAuthors(authors);
    // add inputs

    Set<Input> inputs = new HashSet<>();
    Input inputa = new Input();
    inputa.setName("a");
    inputa.setDisplayName("Operand A");
    inputa.setDescription("First operand");
    IntegerType inputType1_1 = new IntegerType();
    inputType1_1.setId("integer");
    inputa.setType(inputType1_1);

    Input inputb = new Input();
    inputb.setName("b");
    inputb.setDisplayName("Operand B");
    inputb.setDescription("Second operand");
    IntegerType inputType1_2 = new IntegerType();
    inputType1_2.setId("integer");
    inputb.setType(inputType1_2);

    inputs.add(inputa);
    inputs.add(inputb);
    task.setInputs(inputs);
    // add outputs for task one
    Set<Output> outputs = new HashSet<>();
    Output output = new Output();
    output.setName("sum");
    output.setDisplayName("Sum");
    output.setDescription("Sum of operands A and B");
    IntegerType outputType = new IntegerType();
    outputType.setId("integer");
    output.setType(outputType);
    outputs.add(output);
    task.setOutputs(outputs);
    return task;
  }

  public static Task buildHardcodedAddInteger() {
    return buildHardcodedAddInteger(UUID.randomUUID());
  }

  public static Task buildHardcodedSubtractInteger(UUID taskUUID) {
    String storageIdentifierForTaskOne = "task-" + taskUUID + "-def";
    String imageRegistryCompliantNameForTaskOne = "com/cytomine/dummy/arithmetic/integer/subtraction:1.0.0";
    TaskIdentifiers taskIdentifiersForTaskOne = new TaskIdentifiers(taskUUID, storageIdentifierForTaskOne, imageRegistryCompliantNameForTaskOne);

    // store two tasks in the database
    Task task = new Task();
    task.setIdentifier(taskIdentifiersForTaskOne.getLocalTaskIdentifier());
    task.setStorageReference(taskIdentifiersForTaskOne.getStorageIdentifier());
    task.setName("Integers subtraction");
    task.setNameShort("sub_integers");
    task.setDescriptorFile("com.cytomine.dummy.arithmetic.integer.subtraction");
    task.setNamespace("com.cytomine.dummy.arithmetic.integer.subtraction");
    task.setVersion("1.0.0");
    task.setDescription("");
    // add authors
    Set<Author> authors = new HashSet<>();
    Author author = new Author();
    author.setFirstName("Romain");
    author.setLastName("Mormont");
    author.setOrganization("Cytomine Corporation");
    author.setEmail("romain.mormont@cytomine.com");
    author.setContact(true);
    authors.add(author);
    task.setAuthors(authors);
    // add inputs

    Set<Input> inputs = new HashSet<>();
    Input inputa = new Input();
    inputa.setName("a");
    inputa.setDisplayName("Operand A");
    inputa.setDescription("First operand");
    IntegerType inputType1_1 = new IntegerType();
    inputType1_1.setId("integer");
    inputa.setType(inputType1_1);

    Input inputb = new Input();
    inputb.setName("b");
    inputb.setDisplayName("Operand B");
    inputb.setDescription("Second operand");
    IntegerType inputType1_2 = new IntegerType();
    inputType1_2.setId("integer");
    inputb.setType(inputType1_2);

    inputs.add(inputa);
    inputs.add(inputb);
    task.setInputs(inputs);
    // add outputs for task one
    Set<Output> outputs = new HashSet<>();
    Output output = new Output();
    output.setName("out");
    output.setDisplayName("Difference");
    output.setDescription("Difference of operands A and B");
    IntegerType outputType = new IntegerType();
    outputType.setId("integer");
    output.setType(outputType);
    outputs.add(output);
    task.setOutputs(outputs);
    return task;
  }

  public static Task buildHardcodedSubtractInteger() {
    return buildHardcodedSubtractInteger(UUID.randomUUID());
  }
}

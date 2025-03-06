package be.cytomine.appengine.models.task.collection;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.dto.inputs.task.types.collection.CollectionGenericTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.collection.CollectionItemValue;
import be.cytomine.appengine.dto.inputs.task.types.collection.CollectionValue;
import be.cytomine.appengine.dto.inputs.task.types.collection.GeoCollectionValue;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationValue;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.exceptions.ParseException;
import be.cytomine.appengine.exceptions.ProvisioningException;
import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.handlers.StorageData;
import be.cytomine.appengine.handlers.StorageDataEntry;
import be.cytomine.appengine.handlers.StorageDataType;
import be.cytomine.appengine.models.task.Parameter;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.models.task.bool.BooleanPersistence;
import be.cytomine.appengine.models.task.enumeration.EnumerationPersistence;
import be.cytomine.appengine.models.task.geometry.GeometryPersistence;
import be.cytomine.appengine.models.task.geometry.GeometryType;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;
import be.cytomine.appengine.models.task.number.NumberPersistence;
import be.cytomine.appengine.models.task.string.StringPersistence;
import be.cytomine.appengine.repositories.bool.BooleanPersistenceRepository;
import be.cytomine.appengine.repositories.collection.CollectionPersistenceRepository;
import be.cytomine.appengine.repositories.enumeration.EnumerationPersistenceRepository;
import be.cytomine.appengine.repositories.geometry.GeometryPersistenceRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.repositories.number.NumberPersistenceRepository;
import be.cytomine.appengine.repositories.string.StringPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import be.cytomine.appengine.utils.FileHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.geojson.GeoJsonReader;

@SuppressWarnings("checkstyle:LineLength")
@Data
@Entity
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class CollectionType extends Type {

  @Column(nullable = true)
  private Integer minSize;

  @Column(nullable = true)
  private Integer maxSize;

  @Column(nullable = true)
  private String subTypeId;

  @OneToOne(cascade = CascadeType.ALL, optional = false)
  private Type subType;

  @Transient private Type trackingType;

  @Transient private Type parentType;

  public void validateFeatureCollection(String json, GeometryType geometryType)
      throws TypeValidationException {
      ObjectMapper objectMapper = new ObjectMapper();

      JsonNode rootNode = null;
      try {
          rootNode = objectMapper.readTree(json);
      } catch (JsonProcessingException e)
      {
        throw new TypeValidationException("invalid feature collection");
      }

      // Validate "type"
      if (!rootNode.has("type") || !rootNode.get("type").asText().equals("FeatureCollection")) {
        throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
      }

      // Validate "features" array
      if (!rootNode.has("features") || !rootNode.get("features").isArray()) {
        throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
      }

      // validate against constraints
      int size = rootNode.get("features").size();
      if (size < minSize || size > maxSize) {
        throw new TypeValidationException("invalid collection dimensions");
      }

      // Validate each feature
      for (JsonNode feature : rootNode.get("features")) {
        if (!feature.has("type") || !feature.get("type").asText().equals("Feature")) {
          throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
        }
        if (!feature.has("geometry")) {
          throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
        }

        // Validate the geometry using GeometryType
        JsonNode geometryNode = feature.get("geometry");
          try
          {
              geometryType.validate(objectMapper.writeValueAsString(geometryNode));
          } catch (JsonProcessingException e)
          {
            throw new TypeValidationException("invalid feature collection");
          }

      }
  }

  public void validateGeometryCollection(String json, GeometryType geometryType)
      throws TypeValidationException
  {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = null;
      try {
          rootNode = objectMapper.readTree(json);
      } catch (JsonProcessingException e) {
        throw new TypeValidationException("invalid geometry collection");
      }

      // Validate "type"
      if (!rootNode.has("type") || !rootNode.get("type").asText().equals("GeometryCollection")) {
        throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
      }

      // Validate "geometries" array
      if (!rootNode.has("geometries") || !rootNode.get("geometries").isArray()) {
        throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_INVALID_GEOJSON);
      }

      // validate against constraints
      int size = rootNode.get("geometries").size();
      if (size < minSize || size > maxSize) {
        throw new TypeValidationException("invalid collection dimensions");
      }

      // Validate each geometry using GeometryType
      for (JsonNode geometryNode : rootNode.get("geometries")) {
          try
          {
              geometryType.validate(objectMapper.writeValueAsString(geometryNode));
          } catch (JsonProcessingException e){
            throw new TypeValidationException("invalid geometry collection");
          }
      }
  }

  public void setConstraint(CollectionGenericTypeConstraint constraint, String value) {
    switch (constraint) {
      case MIN_SIZE:
        this.setMinSize(Integer.parseInt(value));
        break;
      case MAX_SIZE:
        this.setMaxSize(Integer.parseInt(value));
        break;
      default:
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void validateFiles(
      Run run,
      Parameter currentOutput,
      StorageData currentOutputStorageData)
      throws TypeValidationException
  {
    // make sure we have the right file structure
    Type currentType = new CollectionType(this);
    while (currentType instanceof CollectionType) {
      currentType = ((CollectionType) currentType).getSubType();
    }
    String leafType = currentType.getClass().getCanonicalName();

    if (Objects.isNull(currentOutputStorageData)
        || Objects.isNull(currentOutputStorageData.getEntryList())
        || currentOutputStorageData.getEntryList().isEmpty()) {
        throw new RuntimeException("invalid collection dimensions");
    }
    boolean arrayDotYmlFound = false;
    Map<String,Object> lists = new LinkedHashMap<>();
    for (StorageDataEntry entry : currentOutputStorageData.getEntryList()) {
        String entryName = entry.getName();
        if (entryName.endsWith("/")){ // this is a directory
          boolean relatedToOutputParameter = entryName.startsWith(currentOutput.getName() + "/");
          boolean isOutputParameterMainDirectory = entryName.equals(currentOutput.getName() + "/");
          if (relatedToOutputParameter && isOutputParameterMainDirectory){
            List<Object> nestedItems = new ArrayList<>();
            lists.put(entryName, nestedItems);
          }
        } else {
          if (entryName.endsWith("array.yml")){
            String parentListName = entryName.substring(0, entryName.lastIndexOf('/') + 1);
            Map<String,Object> item = new LinkedHashMap<>();
            String arrayDotYmlContent = FileHelper.read(entry.getData(), getStorageCharset());
            item.put("array.yml", arrayDotYmlContent);
            ((List<Object>) lists.get(parentListName)).add(item);
            continue;
          }
          String parentListName = entryName.substring(0, entryName.lastIndexOf('/') + 1);
          Map<String,Object> item = new LinkedHashMap<>();
          String index = entryName.substring(entryName.lastIndexOf('/') + 1);
          item.put("index", Integer.parseInt(index));
          String value = FileHelper.read(entry.getData(), getStorageCharset());
          switch (leafType){
            case "be.cytomine.appengine.models.task.integer.IntegerType":
              item.put("value" , Integer.parseInt(value));
              break;
            case "be.cytomine.appengine.models.task.string.StringType",
                 "be.cytomine.appengine.models.task.geometry.GeometryType",
                 "be.cytomine.appengine.models.task.enumeration.EnumerationType":
              item.put("value" , value);
              break;
            case "be.cytomine.appengine.models.task.number.NumberType":
              item.put("value" , Double.parseDouble(value));
              break;
            case "be.cytomine.appengine.models.task.bool.BooleanType":
              item.put("value" , Boolean.parseBoolean(value));
              break;
            default: throw new TypeValidationException("unknown leaf type: " + leafType);
          }
          ((List<Object>) lists.get(parentListName)).add(item);
        }
    }
    // check that every list (collection or nested collection has array.yml)
    for (String key : lists.keySet()) {
      List<Object> collection = (List<Object>) lists.get(key);
      boolean arrayYmlFound = false;
      for (Object item : collection) {
        Map<String, Object> itemMap = (Map<String, Object>) item;
        arrayYmlFound = itemMap.containsKey("array.yml");
        }
      if (!arrayYmlFound) {
          throw new TypeValidationException("array.yml not found in collection " + key);
        }
    }

    validate(lists.get(currentOutput.getName()+"/"));

  }

  @Override
  public void validate(Object valueObject) throws TypeValidationException {
    if (valueObject == null) {
      return;
    }
    //
    if (!(valueObject instanceof ArrayList)
        && !(valueObject instanceof String)
        && !(valueObject instanceof LinkedHashMap)) {
      throw new TypeValidationException("wrong provision structure");
    }
    if (valueObject instanceof ArrayList) {
       validateNativeCollection((ArrayList<?>) valueObject);
    }
    // validate a GeoJSON collection like FeatureCollection or GeometryCollection
    if (valueObject instanceof String) {
      validateGeoJSONCollection((String) valueObject);
    }

    // validate collection item
    if (valueObject instanceof LinkedHashMap) {
      validateCollectionItem((LinkedHashMap<?,?>) valueObject);
    }

  }

  private void validateCollectionItem(LinkedHashMap<?,?> valueObject) {
  }

  private void validateGeoJSONCollection(String valueObject) throws TypeValidationException
  {
    // todo : this is not correct I need to redo it
    GeometryType geometryType = new GeometryType();
    validateFeatureCollection(valueObject, geometryType);
    validateGeometryCollection(valueObject,geometryType);

  }

  private void validateNativeCollection(List<?> value) throws TypeValidationException {
    validateNode(value);
  }

  @SuppressWarnings("unchecked")
  public void validateNode(Object obj) throws TypeValidationException
  {
      if (Objects.isNull(trackingType)){
          trackingType = new CollectionType(this);
          parentType = trackingType;
      }else {
          if (trackingType instanceof CollectionType){
              parentType = trackingType;
              trackingType = ((CollectionType) trackingType).getSubType();
          }
      }
    if (trackingType instanceof CollectionType && !(obj instanceof List<?>)){
      throw new RuntimeException("invalid collection dimensions");
    }
    if (obj instanceof List<?>) {
      List<?> list = (List<?>) obj;
        assert trackingType instanceof CollectionType;
        CollectionType currentType = (CollectionType) trackingType;
      if (list.size() < currentType.getMinSize() || list.size() > currentType.getMaxSize()) {
        throw new RuntimeException("invalid collection dimensions");
      }
      for (Object o : list){
          validateNode(o);
      }
    } else {
      Map<String, Object> map = null;
      if (obj instanceof LinkedHashMap) {
        map = (LinkedHashMap<String, Object>) obj;
      }
      assert map != null;
      if (!(trackingType instanceof CollectionType) && map.get("value") instanceof List<?>){
        throw new RuntimeException("wrong provision structure");
      }
      if (map.get("value") instanceof List<?>) {

        if (trackingType instanceof CollectionType) {
          parentType = trackingType;
          trackingType = ((CollectionType) trackingType).getSubType();
        }
        List<?> list = (List<?>) map.get("value");
        CollectionType currentType = (CollectionType) trackingType;
        if (list.size() < currentType.getMinSize() || list.size() > currentType.getMaxSize()) {
          throw new TypeValidationException("invalid collection dimensions");
        }
        for (Object o : list){
            validateNode(o);
        }
        trackingType = parentType;
      }

      // validate subtype
      trackingType.validate(map.get("value"));
      trackingType = parentType;
    }
  }

  @Transactional
  @Override
  public void persistProvision(JsonNode provision, UUID runId) throws ProvisioningException
  {
    persistCollection(provision, runId);

  }

  private void persistCollection(JsonNode provision, UUID runId) throws ProvisioningException
  {
    Type currentType = new CollectionType(this);
    while (currentType instanceof CollectionType) {
      currentType = ((CollectionType) currentType).getSubType();
    }
    CollectionPersistenceRepository collectionRepo =
        AppEngineApplicationContext.getBean(CollectionPersistenceRepository.class);
    String leafType = currentType.getClass().getCanonicalName();
    String parameterName = provision.get("param_name").asText();

    CollectionPersistence collectionPersistence = (CollectionPersistence) persistNode(provision,
        runId, parameterName, leafType);
    collectionRepo.save(collectionPersistence);
  }

  public TypePersistence persistNode(JsonNode node, UUID runId, String parameterName, String leafType)
      throws ProvisioningException
  {
    CollectionPersistenceRepository collectionRepo =
        AppEngineApplicationContext.getBean(CollectionPersistenceRepository.class);
    if (node.isArray()) {
      CollectionPersistence persistedProvision =
          collectionRepo.findCollectionPersistenceByParameterNameAndRunIdAndParameterType(
              parameterName, runId, ParameterType.INPUT);
      if (persistedProvision == null) {
        persistedProvision = new CollectionPersistence();
        persistedProvision.setValueType(ValueType.ARRAY);
        persistedProvision.setParameterType(ParameterType.INPUT);
        persistedProvision.setParameterName(parameterName);
        persistedProvision.setRunId(runId);
      }
      List<TypePersistence> items = new ArrayList<>();
      for (JsonNode item : node) {
        items.add(persistNode(item, runId, parameterName, leafType));
      }
      persistedProvision.setItems(items);
      persistedProvision.setSize(items.size());
      return persistedProvision;
    }
    if (node.isValueNode()){
      // determine the type
      switch (leafType) {
        case "be.cytomine.appengine.models.task.integer.IntegerType":
            return getIntegerPersistence(node, runId, parameterName);
        case "be.cytomine.appengine.models.task.string.StringType":
            return getStringPersistence(node, runId, parameterName);
        case "be.cytomine.appengine.models.task.number.NumberType":
          return getNumberPersistence(node, runId, parameterName);
        case "be.cytomine.appengine.models.task.bool.BooleanType":
          return getBooleanPersistence(node, runId, parameterName);
        case "be.cytomine.apentryValuepengine.models.task.enumeration.EnumerationType":
          return getEnumerationPersistence(node, runId, parameterName);
        case "be.cytomine.appengine.models.task.geometry.GeometryType":
          return getGeometryPersistence(node, runId, parameterName);
      }

    }

    if (node.isObject()) {
      String paramName = "";
      if (Objects.nonNull(node.get("param_name"))){
        paramName = node.get("param_name").asText();
      } else {
        paramName = parameterName+"["+node.get("index").asText()+"]";
      }
      // check if this is GeoJSON collection
      if (node.has("type")
          && (node.get("type").asText().equals("GeometryCollection")
          || node.get("type").asText().equals("FeatureCollection"))){
        CollectionPersistence persistedProvision =
            collectionRepo.findCollectionPersistenceByParameterNameAndRunIdAndParameterType(
                parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
          persistedProvision = new CollectionPersistence();
          persistedProvision.setValueType(ValueType.ARRAY);
          persistedProvision.setParameterType(ParameterType.INPUT);
          persistedProvision.setParameterName(paramName);
          persistedProvision.setRunId(runId);
          persistedProvision.setCompactValue(node.get("value").asText());
          return persistedProvision;
        }
      } else {
        return persistNode(node.get("value"), runId, paramName, leafType);
      }

    }

    if (node.isNull()){
      throw new ProvisioningException("invalid provision value");
    }
    return null;
  }

  private static IntegerPersistence getIntegerPersistence(JsonNode node, UUID runId,
                                                          String parameterName)
  {
    IntegerPersistenceRepository integerPersistenceRepository = AppEngineApplicationContext.getBean(IntegerPersistenceRepository.class);

    IntegerPersistence integerPersistence = integerPersistenceRepository.findIntegerPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (integerPersistence == null) {
      integerPersistence = new IntegerPersistence();
      integerPersistence.setParameterType(ParameterType.INPUT);
      integerPersistence.setParameterName(parameterName);
      integerPersistence.setRunId(runId);
      integerPersistence.setValueType(ValueType.INTEGER);
      integerPersistence.setValue(node.asInt());
      integerPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      integerPersistence.setValue(node.asInt());
    }
    return integerPersistence;
  }

  private static StringPersistence getStringPersistence(JsonNode node, UUID runId,
                                                        String parameterName)
  {
    StringPersistenceRepository stringPersistenceRepository = AppEngineApplicationContext.getBean(
        StringPersistenceRepository.class);

    StringPersistence stringPersistence = stringPersistenceRepository.findStringPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (stringPersistence == null) {
      stringPersistence = new StringPersistence();
      stringPersistence.setParameterType(ParameterType.INPUT);
      stringPersistence.setParameterName(parameterName);
      stringPersistence.setRunId(runId);
      stringPersistence.setValueType(ValueType.STRING);
      stringPersistence.setValue(node.asText());
      stringPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      stringPersistence.setValue(node.asText());
    }
    return stringPersistence;
  }

  private static NumberPersistence getNumberPersistence(JsonNode node, UUID runId,
                                                        String parameterName)
  {
    NumberPersistenceRepository numberPersistenceRepository = AppEngineApplicationContext.getBean(
        NumberPersistenceRepository.class);

    NumberPersistence numberPersistence = numberPersistenceRepository.findNumberPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (numberPersistence == null) {
      numberPersistence = new NumberPersistence();
      numberPersistence.setParameterType(ParameterType.INPUT);
      numberPersistence.setParameterName(parameterName);
      numberPersistence.setRunId(runId);
      numberPersistence.setValueType(ValueType.NUMBER);
      numberPersistence.setValue(node.asDouble());
      numberPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      numberPersistence.setValue(node.asDouble());
    }
    return numberPersistence;
  }

  private static BooleanPersistence getBooleanPersistence(JsonNode node, UUID runId,
                                                          String parameterName)
  {
    BooleanPersistenceRepository booleanPersistenceRepository = AppEngineApplicationContext.getBean(
        BooleanPersistenceRepository.class);

    BooleanPersistence booleanPersistence = booleanPersistenceRepository.findBooleanPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (booleanPersistence == null) {
      booleanPersistence = new BooleanPersistence();
      booleanPersistence.setParameterType(ParameterType.INPUT);
      booleanPersistence.setParameterName(parameterName);
      booleanPersistence.setRunId(runId);
      booleanPersistence.setValueType(ValueType.NUMBER);
      booleanPersistence.setValue(node.asBoolean());
      booleanPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      booleanPersistence.setValue(node.asBoolean());
    }
    return booleanPersistence;
  }

  private static EnumerationPersistence getEnumerationPersistence(JsonNode node, UUID runId,
                                                                          String parameterName)
  {
    EnumerationPersistenceRepository enumerationPersistenceRepository = AppEngineApplicationContext.getBean(
        EnumerationPersistenceRepository.class);

    EnumerationPersistence enumerationPersistence = enumerationPersistenceRepository.findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (enumerationPersistence == null) {
      enumerationPersistence = new EnumerationPersistence();
      enumerationPersistence.setParameterType(ParameterType.INPUT);
      enumerationPersistence.setParameterName(parameterName);
      enumerationPersistence.setRunId(runId);
      enumerationPersistence.setValueType(ValueType.ENUMERATION);
      enumerationPersistence.setValue(node.asText());
      enumerationPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      enumerationPersistence.setValue(node.asText());
    }
    return enumerationPersistence;
  }

  private static GeometryPersistence getGeometryPersistence(JsonNode node, UUID runId,
                                                            String parameterName)
  {
    GeometryPersistenceRepository geometryPersistenceRepository = AppEngineApplicationContext.getBean(
        GeometryPersistenceRepository.class);

    GeometryPersistence geometryPersistence = geometryPersistenceRepository.findGeometryPersistenceByParameterNameAndRunIdAndParameterType(
        parameterName, runId, ParameterType.INPUT
    );
    if (geometryPersistence == null) {
      geometryPersistence = new GeometryPersistence();
      geometryPersistence.setParameterType(ParameterType.INPUT);
      geometryPersistence.setParameterName(parameterName);
      geometryPersistence.setRunId(runId);
      geometryPersistence.setValueType(ValueType.GEOMETRY);
      geometryPersistence.setValue(node.asText());
      geometryPersistence.setCollectionIndex(parameterName.substring(parameterName.indexOf("[")));
    } else {
      geometryPersistence.setValue(node.asText());
    }
    return geometryPersistence;
  }

  @Transactional
  @Override
  public void persistResult(Run run, Parameter currentOutput, StorageData outputValue)
      throws ProvisioningException
  {
     CollectionPersistenceRepository collectionPersistenceRepository =
        AppEngineApplicationContext.getBean(CollectionPersistenceRepository.class);
     CollectionPersistence result = null;

    Type currentType = new CollectionType(this);
    while (currentType instanceof CollectionType) {
      currentType = ((CollectionType) currentType).getSubType();
    }

    String leafType = currentType.getClass().getCanonicalName();
    Map<String, TypePersistence> parameterNameToTypePersistence = new LinkedHashMap<>();
    outputValue.sortShallowToDeep();
    for (StorageDataEntry entry : outputValue.getEntryList()){
      if (entry.getStorageDataType().equals(StorageDataType.DIRECTORY)){
        if (entry.getName().equals(currentOutput.getName()+"/")){
          result = new CollectionPersistence();
          result.setValueType(ValueType.ARRAY);
          result.setParameterType(ParameterType.OUTPUT);
          result.setParameterName(currentOutput.getName());
          result.setRunId(run.getId());
          List<TypePersistence> items = new ArrayList<>();
          result.setItems(items);
          parameterNameToTypePersistence.put(entry.getName(), result);
        }else {
          CollectionPersistence subCollection = new CollectionPersistence();
          String[] nameParts = entry.getName().trim().split("/");
          for(int i = 0; i < nameParts.length; i++) {
            if (i != 0){
              nameParts[i] = "[" + nameParts[i] + "]";
            }
          }

          subCollection.setParameterName(String.join("", nameParts));
          subCollection.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
              Collectors.joining()));
          String parentName = entry.getName().substring(0,entry.getName().lastIndexOf("/")+1);
          // prepare list for sub items
          List<TypePersistence> items = new ArrayList<>();
          subCollection.setItems(items);
          parameterNameToTypePersistence.put(entry.getName(), subCollection);
          // add this collection to parent collection
          CollectionPersistence parentPersistence = (CollectionPersistence) parameterNameToTypePersistence.get(parentName);
          parentPersistence.getItems().add(subCollection);
        }
      }
      if (entry.getStorageDataType().equals(StorageDataType.FILE)){
        String parentName = entry.getName().substring(0,entry.getName().lastIndexOf("/")+1);

        if (entry.getName().endsWith("array.yml")){
          CollectionPersistence parentPersistence = (CollectionPersistence) parameterNameToTypePersistence.get(parentName);
            parentPersistence.setSize(getCollectionSize(entry));
            continue;
        }

        String[] nameParts = entry.getName().trim().split("/");
        for(int i = 0; i < nameParts.length; i++) {
          if (i != 0){
            nameParts[i] = "[" + nameParts[i] + "]";
          }
        }
        CollectionPersistence parentCollection =
            (CollectionPersistence) parameterNameToTypePersistence.get(parentName);
        String entryValue = FileHelper.read(entry.getData(), getStorageCharset());

        if (entry.getName().endsWith(".geojson")){
        // todo handle this file and store it in a CollectionPersistence object then attach it to the parent
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode geoJsonCollectionFileContent;
          try {
                geoJsonCollectionFileContent = objectMapper.readTree(entryValue);
          } catch (JsonProcessingException e) {
              throw new ProvisioningException("invalid format of geojson file " + entry.getName());
          }
          int geoJsonCollectionSize = 0;
          if (geoJsonCollectionFileContent.has("features") && geoJsonCollectionFileContent.get("features").isArray()){
            geoJsonCollectionSize = geoJsonCollectionFileContent.get("features").size();
          }
          if (geoJsonCollectionFileContent.has("geometries") && geoJsonCollectionFileContent.get("geometries").isArray()){
            geoJsonCollectionSize = geoJsonCollectionFileContent.get("geometries").size();
          }
          CollectionPersistence geoJsonCollection = new CollectionPersistence();
          geoJsonCollection.setValueType(ValueType.ARRAY);
          geoJsonCollection.setParameterType(ParameterType.OUTPUT);
          geoJsonCollection.setParameterName(currentOutput.getName());
          geoJsonCollection.setRunId(run.getId());
          geoJsonCollection.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
              Collectors.joining()));
          geoJsonCollection.setSize(geoJsonCollectionSize);
          geoJsonCollection.setCompactValue(entryValue);

          parentCollection.getItems().add(geoJsonCollection);

          continue;
        }

        switch (leafType){
          case "be.cytomine.appengine.models.task.integer.IntegerType":
            IntegerPersistence integerPersistence = new IntegerPersistence();
            integerPersistence.setParameterType(ParameterType.OUTPUT);
            integerPersistence.setRunId(run.getId());
            integerPersistence.setParameterName(String.join("", nameParts));
            integerPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            integerPersistence.setValue(Integer.parseInt(entryValue));
            integerPersistence.setValueType(ValueType.INTEGER);

            parentCollection.getItems().add(integerPersistence);
            break;
          case "be.cytomine.appengine.models.task.string.StringType":
            StringPersistence stringPersistence = new StringPersistence();
            stringPersistence.setParameterType(ParameterType.OUTPUT);
            stringPersistence.setRunId(run.getId());
            stringPersistence.setParameterName(String.join("", nameParts));
            stringPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            stringPersistence.setValue(entryValue);
            stringPersistence.setValueType(ValueType.STRING);

            parentCollection.getItems().add(stringPersistence);
            break;
          case "be.cytomine.appengine.models.task.enumeration.EnumerationType":
            EnumerationPersistence enumerationPersistence = new EnumerationPersistence();
            enumerationPersistence.setParameterType(ParameterType.OUTPUT);
            enumerationPersistence.setRunId(run.getId());
            enumerationPersistence.setParameterName(String.join("", nameParts));
            enumerationPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            enumerationPersistence.setValue(entryValue);
            enumerationPersistence.setValueType(ValueType.ENUMERATION);

            parentCollection.getItems().add(enumerationPersistence);
            break;
          case "be.cytomine.appengine.models.task.geometry.GeometryType":
            GeometryPersistence geoPersistence = new GeometryPersistence();
            geoPersistence.setParameterType(ParameterType.OUTPUT);
            geoPersistence.setRunId(run.getId());
            geoPersistence.setParameterName(String.join("", nameParts));
            geoPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            geoPersistence.setValue(entryValue);
            geoPersistence.setValueType(ValueType.GEOMETRY);

            parentCollection.getItems().add(geoPersistence);
            break;
          case "be.cytomine.appengine.models.task.number.NumberType":
            NumberPersistence numberPersistence = new NumberPersistence();
            numberPersistence.setParameterType(ParameterType.OUTPUT);
            numberPersistence.setRunId(run.getId());
            numberPersistence.setParameterName(String.join("", nameParts));
            numberPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            numberPersistence.setValue(Double.parseDouble(entryValue));
            numberPersistence.setValueType(ValueType.NUMBER);

            parentCollection.getItems().add(numberPersistence);
            break;
          case "be.cytomine.appengine.models.task.bool.BooleanType":
            BooleanPersistence booleanPersistence = new BooleanPersistence();
            booleanPersistence.setParameterType(ParameterType.OUTPUT);
            booleanPersistence.setRunId(run.getId());
            booleanPersistence.setParameterName(String.join("", nameParts));
            booleanPersistence.setCollectionIndex(Arrays.stream(nameParts, 1, nameParts.length).collect(
                Collectors.joining()));
            booleanPersistence.setValue(Boolean.parseBoolean(entryValue));
            booleanPersistence.setValueType(ValueType.BOOLEAN);

            parentCollection.getItems().add(booleanPersistence);
            break;
          default: throw new ProvisioningException("unknown leaf type: " + leafType);
        }
        
      }
    }

      assert result != null;
      collectionPersistenceRepository.save(result);
  }

  @Override
  public StorageData mapToStorageFileData(JsonNode provision) throws FileStorageException
  {
      return mapNode("/"+provision.get("param_name").asText(), provision.get("value"),
          new StorageData());
  }

  private StorageData mapNode(String path, JsonNode value, StorageData container)
      throws FileStorageException
  {
    if (value.isNull()){
      throw new FileStorageException("invalid provision value");
    }
    if (value.isArray()){
      container.add(new StorageDataEntry(path, StorageDataType.DIRECTORY));
      int size = value.size();
      String arrayDotYmpData = "size: " + size;
      container.add(new StorageDataEntry(FileHelper.write("array.yml", arrayDotYmpData.getBytes(StandardCharsets.UTF_8)),
          path+"/"+"array.yml",
          StorageDataType.FILE));
      for (JsonNode item : value){
        container = mapNode(path+"/"+item.get("index").asText(), item.get("value"),container);
      }
    }
    if (value.isObject() || value.isValueNode()) {
      // todo : check if the object is geojson collection
      if (value.has("type")
          && (value.get("type").asText().equals("GeometryCollection")
          || value.get("type").asText().equals("FeatureCollection"))){
        path += ".geojson";
      }
      StorageDataEntry itemFileEntry = new StorageDataEntry(
          FileHelper.write(path.substring(path.lastIndexOf("/")+1), value.asText().getBytes(StandardCharsets.UTF_8)),
          path,
          StorageDataType.FILE
      );
      container.add(itemFileEntry);
    }
    return container;
  }

  @Override
  public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode provisionedParameter = mapper.createObjectNode();
    provisionedParameter.put("param_name", provision.get("param_name").asText());
    provisionedParameter.set("value", provision.get("value"));
    provisionedParameter.put("task_run_id", String.valueOf(run.getId()));
    return provisionedParameter;
  }

  @Override
  public CollectionValue buildTaskRunParameterValue(
      StorageData output, UUID id, String outputName) throws ProvisioningException
  {
    Type currentType = new CollectionType(this);
    while (currentType instanceof CollectionType) {
      currentType = ((CollectionType) currentType).getSubType();
    }
    CollectionValue collectionValue = new CollectionValue();
    String leafType = currentType.getClass().getCanonicalName();
    Map<String, List<TaskRunParameterValue>> itemListsDictionary = new LinkedHashMap<>();

    output.sortShallowToDeep();
    for (StorageDataEntry entry : output.getEntryList()){
      if (entry.getStorageDataType().equals(StorageDataType.DIRECTORY)){
        String outerDirectoryName = outputName + "/";
        if (entry.getName().equals(outerDirectoryName)){
          collectionValue.setTaskRunId(id);
          collectionValue.setType(ValueType.ARRAY);
          collectionValue.setParameterName(outputName);

          List<TaskRunParameterValue> items = new ArrayList<>();
          itemListsDictionary.put(entry.getName(), items);
          collectionValue.setValue(items);
        }else {
         CollectionItemValue collectionItemValue = new CollectionItemValue();
         collectionItemValue.setParameterName(entry.getName());
          // prepare list for sub items
          List<TaskRunParameterValue> items = new ArrayList<>();
          itemListsDictionary.put(entry.getName(), items);
          collectionItemValue.setValue(items);
          // add this collection to parent collection
          itemListsDictionary.get(entry.getName()).add(collectionItemValue);
        }
      }
      if (entry.getStorageDataType().equals(StorageDataType.FILE)){
        if (entry.getName().endsWith("array.yml")){
          continue;
        }

        String entryValue = FileHelper.read(entry.getData(), getStorageCharset());
        String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));

        // todo : check if this is a geojson collection file
        if (entry.getName().endsWith(".geojson")){
          String outerDirectoryName = outputName + "/";
          if (entry.getName().equals(outerDirectoryName)){
             GeoCollectionValue geoJsonCollection = new GeoCollectionValue();
             geoJsonCollection.setTaskRunId(id);
             geoJsonCollection.setType(ValueType.ARRAY);
             geoJsonCollection.setParameterName(outputName);
             geoJsonCollection.setValue(entryValue);

             return geoJsonCollection;
          } else {
            CollectionItemValue geoCollectionAsSubCollection = new CollectionItemValue();
            geoCollectionAsSubCollection.setIndex(Integer.parseInt(fileNameWithoutExtension));
            geoCollectionAsSubCollection.setValue(entryValue);

            itemListsDictionary.get(entry.getName().substring(0,entry.getName().lastIndexOf("/") + 1))
                .add(geoCollectionAsSubCollection);
          }
          continue;
        }

        CollectionItemValue collectionItemValue = new CollectionItemValue();
        collectionItemValue.setIndex(Integer.parseInt(fileName));
        switch (leafType){
          case "be.cytomine.appengine.models.task.integer.IntegerType":
               collectionItemValue.setValue(Integer.parseInt(entryValue));
               collectionItemValue.setType(ValueType.INTEGER);
               break;
          case "be.cytomine.appengine.models.task.string.StringType":
               collectionItemValue.setValue(entryValue);
               collectionItemValue.setType(ValueType.STRING);
               break;
          case "be.cytomine.appengine.models.task.enumeration.EnumerationType":
               collectionItemValue.setValue(entryValue);
               collectionItemValue.setType(ValueType.ENUMERATION);
               break;
          case "be.cytomine.appengine.models.task.geometry.GeometryType":
               collectionItemValue.setValue(entryValue);
               collectionItemValue.setType(ValueType.GEOMETRY);
               break;
          case "be.cytomine.appengine.models.task.number.NumberType":
               collectionItemValue.setValue(Double.parseDouble(entryValue));
               break;
          case "be.cytomine.appengine.models.task.bool.BooleanType":
               collectionItemValue.setValue(Boolean.parseBoolean(entryValue));
               break;
          default: throw new ProvisioningException("unknown leaf type: " + leafType);
        }
        itemListsDictionary.get(entry.getName().substring(0,entry.getName().lastIndexOf("/") + 1)).add(collectionItemValue);

      }
    }
    return collectionValue;
  }

  private int getCollectionSize(StorageDataEntry output) throws ProvisioningException
  {
    String arrayYmlContent = FileHelper.read(output.getData(), getStorageCharset()).trim();
    if (arrayYmlContent.isEmpty()) {
      throw new ProvisioningException("array.yml is missing");
    }
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    JsonNode arrayYml;
    try
      {
          arrayYml = yamlReader.readTree(arrayYmlContent);
      } catch (JsonProcessingException e)
      {
          throw new ProvisioningException("array.yml is malformed");
      }
      return arrayYml.get("size").asInt();
  }

  @Override
  public CollectionValue buildTaskRunParameterValue(TypePersistence typePersistence)
      throws ProvisioningException
  {
    return (CollectionValue) buildNode(typePersistence);
  }

  private TaskRunParameterValue buildNode(TypePersistence typePersistence)
      throws ProvisioningException
  {
    Type currentType = new CollectionType(this);
    while (currentType instanceof CollectionType) {
      currentType = ((CollectionType) currentType).getSubType();
    }
    String leafType = currentType.getClass().getCanonicalName();

    if (typePersistence instanceof CollectionPersistence){
      CollectionPersistence collectionPersistence = (CollectionPersistence) typePersistence;
      // either items or compact value but not both
      // todo : check whether data saved as items or compact value
      if (Objects.nonNull(collectionPersistence.getItems())
          && !collectionPersistence.getItems().isEmpty()
          && Objects.isNull(collectionPersistence.getCompactValue())){

        CollectionValue collectionValue = new CollectionValue();
        collectionValue.setType(ValueType.ARRAY);
        collectionValue.setParameterName(collectionPersistence.getParameterName());
        collectionValue.setTaskRunId(collectionPersistence.getRunId());

        List<TaskRunParameterValue> items = new ArrayList<>();
        collectionValue.setValue(items);
        for (TypePersistence persistence: collectionPersistence.getItems()){
          items.add(buildNode(persistence));
        } return collectionValue;
      } else {
        GeoCollectionValue geoCollectionValue = new GeoCollectionValue();
        geoCollectionValue.setType(ValueType.ARRAY);
        geoCollectionValue.setParameterName(collectionPersistence.getParameterName());
        geoCollectionValue.setTaskRunId(collectionPersistence.getRunId());
        geoCollectionValue.setValue(collectionPersistence.getCompactValue());

        return geoCollectionValue;
      }

    } else {
      CollectionItemValue collectionItemValue = new CollectionItemValue();
      String fileName = typePersistence.getParameterName().substring(typePersistence.getParameterName().lastIndexOf("[") + 1).replace("]" , "");
      collectionItemValue.setIndex(Integer.parseInt(fileName));

      if (typePersistence instanceof IntegerPersistence) {
          collectionItemValue.setValue(((IntegerPersistence) typePersistence).getValue());
          collectionItemValue.setType(ValueType.INTEGER);
        } else if (typePersistence instanceof StringPersistence) {
        collectionItemValue.setValue(((StringPersistence) typePersistence).getValue());
        collectionItemValue.setType(ValueType.STRING);
      } else if (typePersistence instanceof GeometryPersistence) {
        collectionItemValue.setValue(((GeometryPersistence) typePersistence).getValue());
        collectionItemValue.setType(ValueType.GEOMETRY);
      } else if (typePersistence instanceof NumberPersistence) {
        collectionItemValue.setValue(((NumberPersistence) typePersistence).getValue());
        collectionItemValue.setType(ValueType.NUMBER);
      } else if (typePersistence instanceof BooleanPersistence) {
        collectionItemValue.setValue(((BooleanPersistence) typePersistence).isValue());
        collectionItemValue.setType(ValueType.BOOLEAN);
      } else if(typePersistence instanceof EnumerationPersistence){
        collectionItemValue.setValue(((EnumerationPersistence) typePersistence).getValue());
        collectionItemValue.setType(ValueType.ENUMERATION);
      } else {
        throw new ProvisioningException("unknown  type: " + leafType);
      }

      return collectionItemValue;
    }
  }

  // copy constructor

  public CollectionType(CollectionType copy) {
    this.subType = copy.getSubType();
    this.maxSize = copy.getMaxSize();
    this.minSize = copy.getMinSize();
    this.subTypeId = copy.getSubTypeId();
    this.parentType = copy.getParentType();
    this.trackingType = copy.getTrackingType();
  }
}

package be.cytomine.config.nosqlmigration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitialMongodbSetupMigration {

    private final MongoTemplate template;

    public InitialMongodbSetupMigration(MongoTemplate template) {
        this.template = template;
    }

    public void changeSet() {

        log.info("Mongo changeset");

        createCollectionIfNotExists(
            "annotationAction",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("user"), Indexes.ascending("image"), Indexes.descending("created")), new IndexOptions().name("user_1_image_1_created_-1").version(1))
            )
        );

        createCollectionIfNotExists(
            "lastConnection",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("date")), new IndexOptions().name("date_2").version(1).expireAfter(300L, TimeUnit.SECONDS))
            )
        );

        // TODO: we have to migrate old data from *UserPosition with the new location scheme (+ index) see MongoDBDomainTests.persistent_user_position_domain
        createCollectionIfNotExists(
            "lastUserPosition",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("user"), Indexes.ascending("image"), Indexes.ascending("slice"), Indexes.descending("created")), new IndexOptions().name("user_1_image_1_slice_1_created_-1").version(1)),
                new IndexModel(Indexes.compoundIndex(Indexes.geo2d("location"), Indexes.ascending("image"), Indexes.ascending("slice")), new IndexOptions().name("location_2d_image_1_slice_1").version(1).min((double) Integer.MIN_VALUE).max((double) Integer.MAX_VALUE)),
                new IndexModel(Indexes.compoundIndex(Indexes.ascending("created")), new IndexOptions().name("created_1").expireAfter(60L, TimeUnit.SECONDS).version(1)),
                new IndexModel(Indexes.compoundIndex(Indexes.ascending("image")), new IndexOptions().name("image_1").version(1))
            )
        );

        createCollectionIfNotExists(
            "persistentConnection",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("user"),Indexes.descending("created")), new IndexOptions().name("user_1_created_-1").version(1))
            )
        );

        createCollectionIfNotExists(
            "persistentImageConsultation",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("user"), Indexes.ascending("image"),Indexes.descending("created")), new IndexOptions().name("user_1_image_1_created_-1").version(1)),
                new IndexModel(Indexes.ascending("image"), new IndexOptions().name("image_1").version(1))
            )
        );

        createCollectionIfNotExists(
            "persistentProjectConnection",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("project"),Indexes.descending("created")), new IndexOptions().name("project_1_created_-1").version(1)),
                new IndexModel(Indexes.ascending("project"), new IndexOptions().name("project_1").version(1))
            )
        );
        
        createCollectionIfNotExists(
            "persistentUserPosition",
            List.of(
                new IndexModel(Indexes.ascending("_id"), new IndexOptions().name("_id_").version(1)),
                new IndexModel(Indexes.compoundIndex( Indexes.ascending("user"), Indexes.ascending("image"), Indexes.ascending("slice"), Indexes.descending("created")), new IndexOptions().name("user_1_image_1_slice_1_created_-1").version(1)),
                new IndexModel(Indexes.compoundIndex(Indexes.geo2d("location"), Indexes.ascending("image"), Indexes.ascending("slice")), new IndexOptions().name("location_2d_image_1_slice_1").version(1).min((double) Integer.MIN_VALUE).max((double) Integer.MAX_VALUE)),
                new IndexModel(Indexes.compoundIndex(Indexes.ascending("image")), new IndexOptions().name("image_1").version(1))
            )
        );
    }

    public MongoCollection<Document> createCollectionIfNotExists(String name, List<IndexModel> indexes) {
        log.info("check if collection " + name + " exists");
        if (template.collectionExists(name)) {
            return template.getCollection(name);
        } else {
            log.info("create collection with " + indexes.size() + " indexes");
            MongoCollection<Document> collection = template.createCollection(name);
            collection.createIndexes(indexes);
            return collection;
        }
    }
}

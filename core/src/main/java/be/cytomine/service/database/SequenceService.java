package be.cytomine.service.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Transactional
@Service
public class SequenceService {

    public final static String SEQ_NAME = "hibernate_sequence";

    @Autowired
    private EntityManager entityManager;

    /**
     * Get a new id number
     */
    public Long generateID()  {
        try {
            Query query = entityManager.createNativeQuery("select nextval('" + SEQ_NAME + "');");
            return (Long) query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate ID with sequence: " + e, e);
        }
    }
}

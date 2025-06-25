package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.ontology.RelationTermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestRelationTermController extends RestCytomineController {

    private final RelationTermService relationTermService;

    private final TermRepository termRepository;

    private final RelationRepository relationRepository;

    /**
     * List all relation for a specific term and position
     */
    @GetMapping("/relation/term/{i}/{id}.json")
    public ResponseEntity<String> listByTerm(
            @PathVariable Long id,
            @PathVariable Integer i,
            @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list terms");

        if (i!=1 && i!=2) {
            throw new ObjectNotFoundException("'i' must be 1 or 2. Current value is " + i);
        }
        return termRepository.findById(id)
                .map( term -> responseSuccess(relationTermService.list(term, String.valueOf(i))))
                .orElseThrow(() -> new ObjectNotFoundException("Term", id));
    }

    /**
     * List all relation for a specific term and position
     */
    @GetMapping("/relation/term/{id}.json")
    public ResponseEntity<String> listByTermAll(
            @PathVariable Long id,
            @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list terms");
        return termRepository.findById(id)
                .map( term -> responseSuccess(relationTermService.list(term)))
                .orElseThrow(() -> new ObjectNotFoundException("Term", id));
    }


    /**
     * Check if a relation exist with term1 and term2
     */
    @GetMapping("/relation/parent/term1/{idTerm1}/term2/{idTerm2}.json")
    public ResponseEntity<String> show(
            @PathVariable Long idTerm1,
            @PathVariable Long idTerm2
    ) {
        return show(relationRepository.getParent().getId(), idTerm1, idTerm2);
    }

    @GetMapping("/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}.json")
    public ResponseEntity<String> show(
            @PathVariable Long idRelation,
            @PathVariable Long idTerm1,
            @PathVariable Long idTerm2
    ) {
        log.debug("REST request to get relation term {} {} {}", idRelation, idTerm1, idTerm2);
        Relation relation = relationRepository.findById(idRelation)
                .orElseThrow(() -> new ObjectNotFoundException("Relation", idRelation));;
        Term term1 = termRepository.findById(idTerm1)
                .orElseThrow(() -> new ObjectNotFoundException("Term", idTerm1));
        Term term2 = termRepository.findById(idTerm2).
                orElseThrow(() -> new ObjectNotFoundException("Term", idTerm2));

        return relationTermService.find(relation, term1, term2)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Relation Term", Map.of("Relation", idRelation, "Term", idTerm1, "Term2", idTerm2)));
    }


    /**
     * Add a new relation with two terms
     */
    @PostMapping("/relation/{id}/term.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save Term : " + json);
        if (json.isMissing("relation")) {
            json.put("relation", relationRepository.getParent().getId());
        }
        return add(relationTermService, json);
    }

    /**
     * Delete a relation between two terms
     */
    @DeleteMapping("/relation/parent/term1/{idTerm1}/term2/{idTerm2}.json")
    public ResponseEntity<String> delete(
            @PathVariable Long idTerm1,
            @PathVariable Long idTerm2
    ) {
        return delete(relationRepository.getParent().getId(), idTerm1, idTerm2);
    }

    /**
     * Delete a relation between two terms
     */
    @DeleteMapping("/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}.json")
    public ResponseEntity<String> delete(
            @PathVariable Long idRelation,
            @PathVariable Long idTerm1,
            @PathVariable Long idTerm2
    ) {
        log.debug("REST request to delete Relation Term");
        return delete(relationTermService, JsonObject.of("relation", idRelation, "term1", idTerm1, "term2", idTerm2), null);
    }
}

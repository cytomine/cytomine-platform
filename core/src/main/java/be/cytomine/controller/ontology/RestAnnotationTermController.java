package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ontology.*;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.acls.domain.BasePermission.READ;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationTermController extends RestCytomineController {

    private final AnnotationTermService annotationTermService;

    private final TermRepository termRepository;

    private final AnnotationDomainRepository annotationDomainRepository;

    private final UserAnnotationService userAnnotationService;

    private final UserService userService;

    private final TermService termService;

    private final SecurityACLService securityACLService;

    private final ReviewedAnnotationService reviewedAnnotationService;

    private final CurrentUserService currentUserService;


    @GetMapping("/annotation/{idAnnotation}/term.json")
    public ResponseEntity<String> listByAnnotation(
            @PathVariable Long idAnnotation,
            @RequestParam(value = "idUser", required = false) Long idUser
    ) {
        log.debug("REST request to list terms for annotation {}", idAnnotation);
        List results = new ArrayList<>();
        AnnotationDomain annotation = annotationDomainRepository.findById(idAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", idAnnotation));
        if (idUser==null && annotation.isUserAnnotation()) {
            results.addAll(annotationTermService.list((UserAnnotation)annotation));
//        } else if(idUser==null && annotation.isAlgoAnnotation()) {
//            results.addAll(algoAnnotationTermService.list((AlgoAnnotation)annotation));
        } else if(idUser==null && annotation.isReviewedAnnotation()) {
            results.addAll(reviewedAnnotationService.listTerms((ReviewedAnnotation)annotation));
        } else if(idUser!=null) {
            User user = (User)userService.find(idUser).orElseThrow(() -> new ObjectNotFoundException("User", idUser));
            results.addAll(annotationTermService.list((UserAnnotation)annotation, user));
        }
        return responseSuccess(results);
    }

    /**
     * Get all term link with an annotation by all user except  params.idUser
     */
    @GetMapping("/annotation/{idAnnotation}/notuser/{idNotUser}/term.json")
    public ResponseEntity<String> listAnnotationTermByUserNot(
            @PathVariable Long idAnnotation,
            @PathVariable Long idNotUser
    ) {
        log.debug("REST request to list terms for annotation {} not defined by user {}", idAnnotation, idNotUser);
        UserAnnotation annotation = userAnnotationService.find(idAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("UserAnnotation", idAnnotation));
        User user = (User)userService.find(idNotUser)
                .orElseThrow(() -> new ObjectNotFoundException("User", idNotUser));
        return responseSuccess(annotationTermService.listAnnotationTermNotDefinedByUser(annotation, user));
    }

    @GetMapping(value = {"/annotation/{idAnnotation}/term/{idTerm}.json", "/annotation/{idAnnotation}/term/{idTerm}/user/{idUser}.json"})
    public ResponseEntity<String> show(
            @PathVariable Long idAnnotation,
            @PathVariable Long idTerm,
            @PathVariable(required = false) Long idUser
    ) {
        log.debug("REST request to get annotation term with annotation {} term {} user {}", idAnnotation, idTerm, idUser);
        AnnotationDomain annotation = annotationDomainRepository.findById(idAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", idAnnotation));;
        Term term = termService.find(idTerm)
                .orElseThrow(() -> new ObjectNotFoundException("Term", idTerm));

        if (idUser!=null) {
            User user = userService.find(idUser)
                    .orElseThrow(() -> new ObjectNotFoundException("User", idUser));
            //user is set, get a specific annotation-term link from user
//            if (userService.getCurrentUser().isAlgo()) {
//                return responseSuccess(algoAnnotationTermService.find(annotation, term, (UserJob) user)
//                        .orElseThrow(() -> new ObjectNotFoundException("AlgoAnnotationTerm", annotation + "-"+term+"-"+idUser)));
//            } else {
                return responseSuccess(annotationTermService.find(annotation, term, user)
                        .orElseThrow(() -> new ObjectNotFoundException("AnnotationTerm", annotation + "-"+term+"-"+idUser)));
//            }
        } else {
            //user is not set, we will get the annotation-term from all user
//            if(userService.getCurrentUser().isAlgo()) {
//                return responseSuccess(algoAnnotationTermService.find(annotation, term, null)
//                        .orElseThrow(() -> new ObjectNotFoundException("AlgoAnnotationTerm", annotation + "-"+term+"-null")));
//            } else {
                return responseSuccess(annotationTermService.find(annotation, term, null)
                        .orElseThrow(() -> new ObjectNotFoundException("AnnotationTerm", annotation + "-"+term+"-null")));
//            }
        }
    }


    /**
     * Add a new annotation with two terms
     */
    @PostMapping("/annotation/{idAnnotation}/term/{idTerm}.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save annotation term : " + json);

        //Get annotation, it can be a userAnnotation, an algoAnnotation or a Reviewed
        AnnotationDomain annotation =
                annotationDomainRepository.findById(json.get("annotationIdent")!=null ? json.getJSONAttrLong("annotationIdent") : json.getJSONAttrLong("userannotation"))
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", "annotationIdent="+json.getJSONAttrStr("annotationIdent","null") + "-userannotation="+json.getJSONAttrStr("userannotation","null")));
            //if term is added from a user, check if the user has permission for UserAnnotation domain
            securityACLService.check(json.getJSONAttrLong("userannotation"),UserAnnotation.class,READ);
            //Check if user is admin, the project mode and if is the owner of the annotation
            securityACLService.checkFullOrRestrictedForOwner(json.getJSONAttrLong("userannotation"),UserAnnotation.class, "user");
            return responseSuccess(annotationTermService.add(json));
    }


    @DeleteMapping(value = {"/annotation/{idAnnotation}/term/{idTerm}.json", "/annotation/{idAnnotation}/term/{idTerm}/user/{idUser}.json"})
    public ResponseEntity<String> delete(
            @PathVariable Long idAnnotation,
            @PathVariable Long idTerm,
            @PathVariable(required = false) Long idUser
    ) {
        log.debug("REST request to get annotation term with annotation {} term {} user {}", idAnnotation, idTerm, idUser);
        AnnotationDomain annotation = userAnnotationService.find(idAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", idAnnotation));;
        Term term = termService.find(idTerm)
                .orElseThrow(() -> new ObjectNotFoundException("Term", idTerm));
        User user = userService.find(idUser!=null? idUser: -1L)
                .orElseGet(currentUserService::getCurrentUser);
        return delete(annotationTermService, JsonObject.of("userannotation", annotation.getId(), "term", term.getId(), "user", user.getId()), null);
    }


    /**
     * Add annotation-term for an annotation and delete all annotation-term that where already map with this annotation by this user
     */
    @PostMapping("/annotation/{idAnnotation}/term/{idTerm}/clearBefore.json")
    public ResponseEntity<String> addWithDeletingOldTerm(
            @PathVariable Long idAnnotation,
            @PathVariable Long idTerm,
            @RequestParam(required = false, defaultValue = "false") Boolean clearForAll

    ) {
        log.debug("REST request to save annotation term and clean before");
        return responseSuccess(annotationTermService.addWithDeletingOldTerm(idAnnotation, idTerm, clearForAll));
    }
}

package be.cytomine.authorization.project;

import java.util.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.User;
import be.cytomine.repository.project.ProjectRepresentativeUserRepository;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.meta.DescriptionService;
import be.cytomine.service.meta.PropertyService;
import be.cytomine.service.meta.TagDomainAssociationService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectRepresentativeUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.util.*;

import static be.cytomine.domain.project.EditingMode.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ProjectAuthorizationTest extends CRUDAuthorizationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private ProjectRepresentativeUserService projectRepresentativeUserService;

    @Autowired
    UserService userService;

    @Autowired
    private ProjectRepresentativeUserRepository projectRepresentativeUserRepository;

    @Autowired
    private DescriptionService descriptionService;

    @Autowired
    private AttachedFileService attachedFileService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private TagDomainAssociationService tagDomainAssociationService;

    @Autowired
    private UserAnnotationService userAnnotationService;

    private Project project = null;

    @BeforeEach
    public void before() throws Exception {
        if (project == null) {
            project = builder.given_a_project();

            initACL(project);
        }
        project.setMode(CLASSIC);
        builder.persistAndReturn(project);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_projects() {
        assertThat(projectService.list(null, new ProjectSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L)
                .stream().map(x -> x.get("id")))
                .contains(project.getId());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_at_least_read_permission_can_list_projects(){
        assertThat(projectService.list((User) userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get(), new ProjectSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L)
                .stream().map(x -> x.get("id")))
                .contains(project.getId());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_projects(){
        expectForbidden(() -> {
            projectService.list(null, new ProjectSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L).stream().map(x -> x.get("id"));
        });
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_user_to_project() {
        expectOK(() -> {userService.addUserToProject(builder.given_a_user(), project, true); });
        expectOK(() -> {userService.addUserToProject(builder.given_a_user(), project, false); });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_rigth_can_manage_user_in_project(){
        expectOK(() -> {userService.addUserToProject(builder.given_a_user(), project, true); });
        expectOK(() -> {userService.addUserToProject(builder.given_a_user(), project, false); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_acl_cannot_manage_user_in_project(){
        expectForbidden(() -> {userService.addUserToProject(builder.given_a_user(), project, true); });
        expectForbidden(() -> {userService.addUserToProject(builder.given_a_user(), project, false); });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void classic_project_scenario_for_admin(){

        expectOK(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectOK(() -> {userService.addUserToProject(user, project, false); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, false); });
        expectOK(() -> {userService.addUserToProject(user, project, true); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, true); });

        expectOK(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );

        // add another representative so that we can delete the first one
        expectOK(() -> {userService.addUserToProject(builder.given_superadmin(), project, false); });
        expectOK(() -> {projectRepresentativeUserService.add(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ).toJsonObject());});

        expectOK(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        expectOK(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUserService.find(project, user).get(), null, null, false
        );});

        expectOK(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectOK(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectOK(() -> {propertyService.add(builder.given_a_not_persisted_property(project, "xxxx", "xxxxxx").toJsonObject());});
        expectOK(() -> {tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void classic_project_scenario_for_user(){

        expectForbidden(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectForbidden(() -> {userService.addUserToProject(user, project, false); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, false); });
        expectForbidden(() -> {userService.addUserToProject(user, project, true); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, true); });

        expectForbidden(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );

        // add another representative so that we can try to delete the first one
        projectRepresentativeUserRepository.save(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ));

        expectForbidden(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        builder.persistAndReturn(projectRepresentativeUser);
        expectForbidden(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUserService.find(project, user).get(), null, null, false
        );});

        expectForbidden(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectForbidden(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectForbidden(() -> {propertyService.add(builder.given_a_not_persisted_property(project, UUID.randomUUID().toString(), "value").toJsonObject());});
        expectForbidden(() -> {tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void classic_project_with_image_as_contributor(){
       //Force project to Read and write
        project.setMode(CLASSIC);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyUser, propertyUser.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyUser, null, null, false));

        //add,update, delete property (admin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyAdmin, null, null, false));

        //add,update, delete property (superadmin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(property, property.toJsonObject()));
        expectOK(() -> propertyService.delete(property, null, null, false));

        //add,update, delete description (simple user data)
        expectOK(() -> descriptionService.update(descriptionUser, descriptionUser.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionUser, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()));

        //add,update, delete description (admin data)
        expectOK(() -> descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionAdmin, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()));

        //add,update, delete description (superadmin data)
        expectOK(() -> descriptionService.update(description, description.toJsonObject()));
        expectOK(() -> descriptionService.delete(description, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()));

        //add,update, delete tagDomainAssociation (simple user data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaUser, null, null, false));

        //add,update, delete tagDomainAssociation (admin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaAdmin, null, null, false));

        //add,update, delete tagDomainAssociation (superadmin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotation).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tda, null, null, false));

        //add,update, delete attachedFile (simple user data)
        expectOK(() -> attachedFileService.delete(attachedFileUser, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()));

        //add,update, delete attachedFile (admin data)
        expectOK(() -> attachedFileService.delete(attachedFileAdmin, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()));

        //add,update, delete attachedFile (superadmin data)
        expectOK(() -> attachedFileService.delete(attachedFile, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()));

        //start reviewing image (simple user data)
        expectOK(() -> imageInstanceService.startReview(imageUser));
        expectOK(() -> imageInstanceService.stopReview(imageUser, false));

        //start reviewing image (admin data)
        expectOK(() -> imageInstanceService.startReview(imageAdmin));
        expectOK(() -> imageInstanceService.stopReview(imageAdmin, false));

        //start reviewing image (superadmin data)
        expectOK(() -> imageInstanceService.startReview(image));
        expectOK(() -> imageInstanceService.stopReview(image, false));

        //add annotation on my layer
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()));
        //add annotation on other layers
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceUser).toJsonObject()));
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()));

        //update, delete annotation (simple user data)
        expectOK(() -> userAnnotationService.update(annotationUser, annotationUser.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationUser, null, null, false));

        //update, delete annotation (admin data)
        expectOK(() -> userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationAdmin, null, null, false));

        //update, delete annotation (super admin data)
        expectOK(() -> userAnnotationService.update(annotation, annotation.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotation, null, null, false));

        //add image instance
        expectOK(() -> imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()));

        //update, delete image instance (simple user data)
        expectOK(() -> imageInstanceService.update(imageUser, imageUser.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageUser, null, null, false));

        //update, delete image instance (admin data)
        expectOK(() -> imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageAdmin, null, null, false));

        //update, delete image instance (superadmin data)
        expectOK(() -> imageInstanceService.update(image, image.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(image, null, null, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void classic_project_with_image_as_project_manager(){
        //Force project to Read and write
        project.setMode(CLASSIC);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyUser, propertyUser.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyUser, null, null, false));

        //add,update, delete property (admin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyAdmin, null, null, false));

        //add,update, delete property (superadmin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(property, property.toJsonObject()));
        expectOK(() -> propertyService.delete(property, null, null, false));

        //add,update, delete description (simple user data)
        expectOK(() -> descriptionService.update(descriptionUser, descriptionUser.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionUser, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()));

        //add,update, delete description (admin data)
        expectOK(() -> descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionAdmin, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()));

        //add,update, delete description (superadmin data)
        expectOK(() -> descriptionService.update(description, description.toJsonObject()));
        expectOK(() -> descriptionService.delete(description, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()));

        //add,update, delete tagDomainAssociation (simple user data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaUser, null, null, false));

        //add,update, delete tagDomainAssociation (admin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaAdmin, null, null, false));

        //add,update, delete tagDomainAssociation (superadmin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotation).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tda, null, null, false));

        //add,update, delete attachedFile (simple user data)
        expectOK(() -> attachedFileService.delete(attachedFileUser, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()));

        //add,update, delete attachedFile (admin data)
        expectOK(() -> attachedFileService.delete(attachedFileAdmin, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()));

        //add,update, delete attachedFile (superadmin data)
        expectOK(() -> attachedFileService.delete(attachedFile, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()));

        //start reviewing image (simple user data)
        expectOK(() -> imageInstanceService.startReview(imageUser));
        expectOK(() -> imageInstanceService.stopReview(imageUser, false));

        //start reviewing image (admin data)
        expectOK(() -> imageInstanceService.startReview(imageAdmin));
        expectOK(() -> imageInstanceService.stopReview(imageAdmin, false));

        //start reviewing image (superadmin data)
        expectOK(() -> imageInstanceService.startReview(image));
        expectOK(() -> imageInstanceService.stopReview(image, false));

        //add annotation on my layer
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()));
        //add annotation on other layers
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceUser).toJsonObject()));
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()));

        //update, delete annotation (simple user data)
        expectOK(() -> userAnnotationService.update(annotationUser, annotationUser.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationUser, null, null, false));

        //update, delete annotation (admin data)
        expectOK(() -> userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationAdmin, null, null, false));

        //update, delete annotation (super admin data)
        expectOK(() -> userAnnotationService.update(annotation, annotation.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotation, null, null, false));

        //add image instance
        expectOK(() -> imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()));

        //update, delete image instance (simple user data)
        expectOK(() -> imageInstanceService.update(imageUser, imageUser.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageUser, null, null, false));

        //update, delete image instance (admin data)
        expectOK(() -> imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageAdmin, null, null, false));

        //update, delete image instance (superadmin data)
        expectOK(() -> imageInstanceService.update(image, image.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(image, null, null, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void restricted_project_scenario_for_admin(){
        project.setMode(RESTRICTED);
        builder.persistAndReturn(project);
        expectOK(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectOK(() -> {userService.addUserToProject(user, project, false); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, false); });
        expectOK(() -> {userService.addUserToProject(user, project, true); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, true); });

        expectOK(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );



        // add another representative so that we can delete the first one
        expectOK(() -> {userService.addUserToProject(builder.given_superadmin(), project, false); });
        expectOK(() -> {projectRepresentativeUserService.add(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ).toJsonObject());});

        expectOK(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        expectOK(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUserService.find(project, user).get(), null, null, false
        );});

        expectOK(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectOK(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectOK(() -> {propertyService.add(builder.given_a_not_persisted_property(project,"xxxxxx","yyy").toJsonObject());});
        expectOK(() -> {tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void restricted_project_scenario_for_user(){
        project.setMode(RESTRICTED);
        builder.persistAndReturn(project);

        expectForbidden(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectForbidden(() -> {userService.addUserToProject(user, project, false); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, false); });
        expectForbidden(() -> {userService.addUserToProject(user, project, true); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, true); });

        expectForbidden(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );

        // add another representative so that we can try to delete the first one
        projectRepresentativeUserRepository.save(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ));

        expectForbidden(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        builder.persistAndReturn(projectRepresentativeUser);
        expectForbidden(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUser, null, null, false
        );});

        //Description check if not readonly mode, other metadata stick to Write permission. To fix
        expectForbidden(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectForbidden(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectForbidden(() -> {propertyService.add(builder.given_a_property(project).toJsonObject());});
        expectForbidden(() -> {tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void restricted_project_with_image_as_contributor(){
        //Force project to Read and write
        project.setMode(RESTRICTED);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyUser, propertyUser.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyUser, null, null, false));

        //add,update, delete property (admin data)
        expectForbidden(() -> propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()));
        expectForbidden(() -> propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()));
        expectForbidden(() -> propertyService.delete(propertyAdmin, null, null, false));

        //add,update, delete property (superadmin data)
        expectForbidden(() -> propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()));
        expectForbidden(() -> propertyService.update(property, property.toJsonObject()));
        expectForbidden(() -> propertyService.delete(property, null, null, false));

        //add,update, delete description (simple user data)
        expectOK(() -> descriptionService.update(descriptionUser, descriptionUser.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionUser, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()));
        //TODO description doesn't have a user or creator field. Doesn't check neither if admin or not so all is 200
//        //add,update, delete description (admin data)
//        expectOK(() -> { descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()); });
//        expectOK(() -> { descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()); });
//        expectOK(() -> { descriptionService.delete(descriptionAdmin, null, null, false); });
//
//        //add,update, delete description (superadmin data)
//        expectOK(() -> { descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()); });
//        expectOK(() -> { descriptionService.update(description, description.toJsonObject()); });
//        expectOK(() -> { descriptionService.delete(description, null, null, false); });

        //add,update, delete tagDomainAssociation (simple user data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaUser, null, null, false));

        //add,update, delete tagDomainAssociation (admin data)
        expectForbidden(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()));
        expectForbidden(() -> tagDomainAssociationService.delete(tdaAdmin, null, null, false));

        //add,update, delete tagDomainAssociation (superadmin data)
        expectForbidden(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotation).toJsonObject()));
        expectForbidden(() -> tagDomainAssociationService.delete(tda, null, null, false));

        //add,update, delete attachedFile (simple user data)
        expectOK(() -> attachedFileService.delete(attachedFileUser, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()));

        //add,update, delete attachedFile (admin data)
        expectForbidden(() -> attachedFileService.delete(attachedFileAdmin, null, null, false));
        expectForbidden(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()));

        //add,update, delete attachedFile (superadmin data)
        expectForbidden(() -> attachedFileService.delete(attachedFile, null, null, false));
        expectForbidden(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()));

        //start reviewing image (simple user data)
        expectOK(() -> imageInstanceService.startReview(imageUser));

        //start reviewing image (admin data)
        expectForbidden(() -> imageInstanceService.startReview(imageAdmin));

        //start reviewing image (superadmin data)
        expectForbidden(() -> imageInstanceService.startReview(image));

        //add annotation on my layer
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceUser, userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get()).toJsonObject()));

        //add annotation on other layers
        expectForbidden(() -> userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()));
        expectForbidden(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()));

        //update, delete annotation (simple user data)
        expectOK(() -> userAnnotationService.update(annotationUser, annotationUser.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationUser, null, null, false));

        //update, delete annotation (admin data)
        expectForbidden(() -> userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()));
        expectForbidden(() -> userAnnotationService.delete(annotationAdmin, null, null, false));

        //update, delete annotation (super admin data)
        expectForbidden(() -> userAnnotationService.update(annotation, annotation.toJsonObject()));
        expectForbidden(() -> userAnnotationService.delete(annotation, null, null, false));

        //add image instance
        expectOK(() -> imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()));

        //update, delete image instance (simple user data)
        expectOK(() -> imageInstanceService.update(imageUser, imageUser.toJsonObject()));

        expectOK(() -> imageInstanceService.delete(imageUser, null, null, false));

        //update, delete image instance (admin data)
        expectForbidden(() -> imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()));
        expectForbidden(() -> imageInstanceService.delete(imageAdmin, null, null, false));

        //update, delete image instance (superadmin data)
        expectForbidden(() -> imageInstanceService.update(image, image.toJsonObject()));
        expectForbidden(() -> imageInstanceService.delete(image, null, null, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void restricted_project_with_image_as_project_manager(){
        //Force project to Read and write
        project.setMode(RESTRICTED);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyUser, propertyUser.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyUser, null, null, false));

        //add,update, delete property (admin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyAdmin, null, null, false));

        //add,update, delete property (superadmin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(property, property.toJsonObject()));
        expectOK(() -> propertyService.delete(property, null, null, false));

        //add,update, delete description (simple user data)
        expectOK(() -> descriptionService.update(descriptionUser, descriptionUser.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionUser, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()));

        //add,update, delete description (admin data)
        expectOK(() -> descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionAdmin, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()));

        //add,update, delete description (superadmin data)
        expectOK(() -> descriptionService.update(description, description.toJsonObject()));
        expectOK(() -> descriptionService.delete(description, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()));

        //add,update, delete tagDomainAssociation (simple user data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaUser, null, null, false));

        //add,update, delete tagDomainAssociation (admin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaAdmin, null, null, false));

        //add,update, delete tagDomainAssociation (superadmin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotation).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tda, null, null, false));

        //add,update, delete attachedFile (simple user data)
        expectOK(() -> attachedFileService.delete(attachedFileUser, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()));

        //add,update, delete attachedFile (admin data)
        expectOK(() -> attachedFileService.delete(attachedFileAdmin, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()));

        //add,update, delete attachedFile (superadmin data)
        expectOK(() -> attachedFileService.delete(attachedFile, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()));

        //start reviewing image (simple user data)
        expectOK(() -> imageInstanceService.startReview(imageUser));
        expectOK(() -> imageInstanceService.stopReview(imageUser, false));

        //start reviewing image (admin data)
        expectOK(() -> imageInstanceService.startReview(imageAdmin));
        expectOK(() -> imageInstanceService.stopReview(imageAdmin, false));

        //start reviewing image (superadmin data)
        expectOK(() -> imageInstanceService.startReview(image));
        expectOK(() -> imageInstanceService.stopReview(image, false));

        //add annotation on my layer
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()));
        //add annotation on other layers
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceUser).toJsonObject()));
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()));

        //update, delete annotation (simple user data)
        expectOK(() -> userAnnotationService.update(annotationUser, annotationUser.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationUser, null, null, false));

        //update, delete annotation (admin data)
        expectOK(() -> userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationAdmin, null, null, false));

        //update, delete annotation (super admin data)
        expectOK(() -> userAnnotationService.update(annotation, annotation.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotation, null, null, false));

        //add image instance
        expectOK(() -> imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()));

        //update, delete image instance (simple user data)
        expectOK(() -> imageInstanceService.update(imageUser, imageUser.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageUser, null, null, false));

        //update, delete image instance (admin data)
        expectOK(() -> imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageAdmin, null, null, false));

        //update, delete image instance (superadmin data)
        expectOK(() -> imageInstanceService.update(image, image.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(image, null, null, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void readonly_project_scenario_for_admin(){
        project.setMode(READ_ONLY);
        builder.persistAndReturn(project);
        expectOK(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectOK(() -> {userService.addUserToProject(user, project, false); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, false); });
        expectOK(() -> {userService.addUserToProject(user, project, true); });
        expectOK(() -> {userService.deleteUserFromProject(user, project, true); });

        expectOK(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );

        // add another representative so that we can delete the first one
        expectOK(() -> {userService.addUserToProject(builder.given_superadmin(), project, false); });
        expectOK(() -> {projectRepresentativeUserService.add(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ).toJsonObject());});

        expectOK(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        expectOK(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUserService.find(project, user).get(), null, null, false
        );});

        expectOK(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectOK(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectOK(() -> {propertyService.add(builder.given_a_property(project).toJsonObject());});
        expectOK(() -> {tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void readonly_project_scenario_for_user(){
        project.setMode(READ_ONLY);
        builder.persistAndReturn(project);

        expectForbidden(() -> {when_i_edit_domain();});

        User user = builder.given_a_user();
        expectForbidden(() -> {userService.addUserToProject(user, project, false); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, false); });
        expectForbidden(() -> {userService.addUserToProject(user, project, true); });
        expectForbidden(() -> {userService.deleteUserFromProject(user, project, true); });

        expectForbidden(() -> {userService.addUserToProject(user, project, true); });

        ProjectRepresentativeUser projectRepresentativeUser = builder.given_a_not_persisted_project_representative_user(
                project, user
        );

        // add another representative so that we can try to delete the first one
        projectRepresentativeUserRepository.save(builder.given_a_not_persisted_project_representative_user(
                project, builder.given_superadmin()
        ));

        expectForbidden(() -> {projectRepresentativeUserService.add(projectRepresentativeUser.toJsonObject());});
        builder.persistAndReturn(projectRepresentativeUser);
        expectForbidden(() -> {projectRepresentativeUserService.delete(
                projectRepresentativeUser, null, null, false
        );});

        expectForbidden(() -> {descriptionService.add(builder.given_a_not_persisted_description(project).toJsonObject());});
        expectForbidden(() -> {attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());});
        expectForbidden(() -> {propertyService.add(builder.given_a_property(project).toJsonObject());});
        expectForbidden(() -> {tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), project).toJsonObject());});
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void read_only_project_with_image_as_contributor(){
        //Force project to Read and write
        project.setMode(READ_ONLY);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectForbidden(() -> { propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()); });
        expectForbidden(() -> { propertyService.update(propertyUser, propertyUser.toJsonObject()); });
        expectForbidden(() -> { propertyService.delete(propertyUser, null, null, false); });

        //add,update, delete property (admin data)
        expectForbidden(() -> { propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()); });
        expectForbidden(() -> { propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()); });
        expectForbidden(() -> { propertyService.delete(propertyAdmin, null, null, false); });

        //add,update, delete property (superadmin data)
        expectForbidden(() -> { propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()); });
        expectForbidden(() -> { propertyService.update(property, property.toJsonObject()); });
        expectForbidden(() -> { propertyService.delete(property, null, null, false); });

        //add,update, delete description (simple user data)
        expectForbidden(() -> { descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()); });
        expectForbidden(() -> { descriptionService.update(descriptionUser, descriptionUser.toJsonObject()); });
        expectForbidden(() -> { descriptionService.delete(descriptionUser, null, null, false); });

        //add,update, delete description (admin data)
        expectForbidden(() -> { descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()); });
        expectForbidden(() -> { descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()); });
        expectForbidden(() -> { descriptionService.delete(descriptionAdmin, null, null, false); });

        //add,update, delete description (superadmin data)
        expectForbidden(() -> { descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()); });
        expectForbidden(() -> { descriptionService.update(description, description.toJsonObject()); });
        expectForbidden(() -> { descriptionService.delete(description, null, null, false); });

        //add,update, delete tagDomainAssociation (simple user data)
        expectForbidden(() -> { tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()); });
        expectForbidden(() -> { tagDomainAssociationService.delete(tdaUser, null, null, false); });

        //add,update, delete tagDomainAssociation (admin data)
        expectForbidden(() -> { tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()); });
        expectForbidden(() -> { tagDomainAssociationService.delete(tdaAdmin, null, null, false); });

        //add,update, delete tagDomainAssociation (superadmin data)
        expectForbidden(() -> { tagDomainAssociationService.add(builder.given_a_tag_association(builder.given_a_tag(), annotation).toJsonObject()); });
        expectForbidden(() -> { tagDomainAssociationService.delete(tda, null, null, false); });

        //add,update, delete attachedFile (simple user data)
        expectForbidden(() -> { attachedFileService.delete(attachedFileUser, null, null, false); });
        expectForbidden(() -> { attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()); });

        //add,update, delete attachedFile (admin data)
        expectForbidden(() -> { attachedFileService.delete(attachedFileAdmin, null, null, false); });
        expectForbidden(() -> { attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()); });

        //add,update, delete attachedFile (superadmin data)
        expectForbidden(() -> { attachedFileService.delete(attachedFile, null, null, false); });
        expectForbidden(() -> { attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()); });

        //start reviewing image (simple user data)
        expectForbidden(() -> { imageInstanceService.startReview(imageUser); });

        //start reviewing image (admin data)
        expectForbidden(() -> { imageInstanceService.startReview(imageAdmin); });

        //start reviewing image (superadmin data)
        expectForbidden(() -> { imageInstanceService.startReview(image); });

        //add annotation on my layer
        expectForbidden(() -> { userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()); });
        //add annotation on other layers
        expectForbidden(() -> { userAnnotationService.add(builder.given_a_user_annotation(sliceUser).toJsonObject()); });
        expectForbidden(() -> { userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()); });

        //update, delete annotation (simple user data)
        expectForbidden(() -> { userAnnotationService.update(annotationUser, annotationUser.toJsonObject()); });
        expectForbidden(() -> { userAnnotationService.delete(annotationUser, null, null, false); });

        //update, delete annotation (admin data)
        expectForbidden(() -> { userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()); });
        expectForbidden(() -> { userAnnotationService.delete(annotationAdmin, null, null, false); });

        //update, delete annotation (super admin data)
        expectForbidden(() -> { userAnnotationService.update(annotation, annotation.toJsonObject()); });
        expectForbidden(() -> { userAnnotationService.delete(annotation, null, null, false); });

        //add image instance
        expectForbidden(() -> { imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()); });

        //update, delete image instance (simple user data)
        expectForbidden(() -> { imageInstanceService.update(imageUser, imageUser.toJsonObject()); });
        expectForbidden(() -> { imageInstanceService.delete(imageUser, null, null, false); });

        //update, delete image instance (admin data)
        expectForbidden(() -> { imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()); });
        expectForbidden(() -> { imageInstanceService.delete(imageAdmin, null, null, false); });

        //update, delete image instance (superadmin data)
        expectForbidden(() -> { imageInstanceService.update(image, image.toJsonObject()); });
        expectForbidden(() -> { imageInstanceService.delete(image, null, null, false); });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void read_only_project_with_image_as_project_manager(){
        //Force project to Read and write
        project.setMode(RESTRICTED);
        builder.persistAndReturn(project);

        Map<String, Object> data = initProjectDataSet(project);

        //super admin data
        ImageInstance image = (ImageInstance) data.get("image");
        SliceInstance slice = (SliceInstance) data.get("slice");
        UserAnnotation annotation = (UserAnnotation) data.get("annotation");
        Description description = (Description) data.get("description");
        Property property = (Property) data.get("property");
        AttachedFile attachedFile = (AttachedFile) data.get("attachedFile");
        TagDomainAssociation tda = (TagDomainAssociation) data.get("tagDomainAssociation");

        //admin data
        ImageInstance imageAdmin = (ImageInstance) data.get("imageAdmin");
        SliceInstance sliceAdmin = (SliceInstance) data.get("sliceAdmin");
        UserAnnotation annotationAdmin = (UserAnnotation) data.get("annotationAdmin");
        Description descriptionAdmin = (Description) data.get("descriptionAdmin");
        Property propertyAdmin = (Property) data.get("propertyAdmin");
        AttachedFile attachedFileAdmin = (AttachedFile) data.get("attachedFileAdmin");
        TagDomainAssociation tdaAdmin = (TagDomainAssociation) data.get("tagDomainAssociationAdmin");

        //simple user data
        ImageInstance imageUser = (ImageInstance) data.get("imageUser");
        SliceInstance sliceUser = (SliceInstance) data.get("sliceUser");
        UserAnnotation annotationUser = (UserAnnotation) data.get("annotationUser");
        Description descriptionUser = (Description) data.get("descriptionUser");
        Property propertyUser = (Property) data.get("propertyUser");
        AttachedFile attachedFileUser = (AttachedFile) data.get("attachedFileUser");
        TagDomainAssociation tdaUser = (TagDomainAssociation) data.get("tagDomainAssociationUser");

        //add,update, delete property (simple user data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationUser, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyUser, propertyUser.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyUser, null, null, false));

        //add,update, delete property (admin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotationAdmin, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(propertyAdmin, propertyAdmin.toJsonObject()));
        expectOK(() -> propertyService.delete(propertyAdmin, null, null, false));

        //add,update, delete property (superadmin data)
        expectOK(() -> propertyService.add(builder.given_a_not_persisted_property(annotation, "xxx", "value").toJsonObject()));
        expectOK(() -> propertyService.update(property, property.toJsonObject()));
        expectOK(() -> propertyService.delete(property, null, null, false));

        //add,update, delete description (simple user data)
        expectOK(() -> descriptionService.update(descriptionUser, descriptionUser.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionUser, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationUser).toJsonObject()));

        //add,update, delete description (admin data)
        expectOK(() -> descriptionService.update(descriptionAdmin, descriptionAdmin.toJsonObject()));
        expectOK(() -> descriptionService.delete(descriptionAdmin, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotationAdmin).toJsonObject()));

        //add,update, delete description (superadmin data)
        expectOK(() -> descriptionService.update(description, description.toJsonObject()));
        expectOK(() -> descriptionService.delete(description, null, null, false));
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(annotation).toJsonObject()));

        //add,update, delete tagDomainAssociation (simple user data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationUser).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaUser, null, null, false));

        //add,update, delete tagDomainAssociation (admin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotationAdmin).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tdaAdmin, null, null, false));

        //add,update, delete tagDomainAssociation (superadmin data)
        expectOK(() -> tagDomainAssociationService.add(builder.given_a_not_persisted_tag_association(builder.given_a_tag(), annotation).toJsonObject()));
        expectOK(() -> tagDomainAssociationService.delete(tda, null, null, false));

        //add,update, delete attachedFile (simple user data)
        expectOK(() -> attachedFileService.delete(attachedFileUser, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationUser.getId(), annotationUser.getClass().getName()));

        //add,update, delete attachedFile (admin data)
        expectOK(() -> attachedFileService.delete(attachedFileAdmin, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotationAdmin.getId(), annotationAdmin.getClass().getName()));

        //add,update, delete attachedFile (superadmin data)
        expectOK(() -> attachedFileService.delete(attachedFile, null, null, false));
        expectOK(() -> attachedFileService.create("test.txt", new String("hello").getBytes(), "test", annotation.getId(), annotation.getClass().getName()));

        //start reviewing image (simple user data)
        expectOK(() -> imageInstanceService.startReview(imageUser));
        expectOK(() -> imageInstanceService.stopReview(imageUser, false));

        //start reviewing image (admin data)
        expectOK(() -> imageInstanceService.startReview(imageAdmin));
        expectOK(() -> imageInstanceService.stopReview(imageAdmin, false));

        //start reviewing image (superadmin data)
        expectOK(() -> imageInstanceService.startReview(image));
        expectOK(() -> imageInstanceService.stopReview(image, false));

        //add annotation on my layer
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(slice).toJsonObject()));
        //add annotation on other layers
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceUser).toJsonObject()));
        expectOK(() -> userAnnotationService.add(builder.given_a_user_annotation(sliceAdmin).toJsonObject()));

        //update, delete annotation (simple user data)
        expectOK(() -> userAnnotationService.update(annotationUser, annotationUser.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationUser, null, null, false));

        //update, delete annotation (admin data)
        expectOK(() -> userAnnotationService.update(annotationAdmin, annotationAdmin.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotationAdmin, null, null, false));

        //update, delete annotation (super admin data)
        expectOK(() -> userAnnotationService.update(annotation, annotation.toJsonObject()));
        expectOK(() -> userAnnotationService.delete(annotation, null, null, false));

        //add image instance
        expectOK(() -> imageInstanceService.add(builder.given_a_not_persisted_image_instance(project).toJsonObject()));

        //update, delete image instance (simple user data)
        expectOK(() -> imageInstanceService.update(imageUser, imageUser.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageUser, null, null, false));

        //update, delete image instance (admin data)
        expectOK(() -> imageInstanceService.update(imageAdmin, imageAdmin.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(imageAdmin, null, null, false));

        //update, delete image instance (superadmin data)
        expectOK(() -> imageInstanceService.update(image, image.toJsonObject()));
        expectOK(() -> imageInstanceService.delete(image, null, null, false));
    }

    private Map<String, Object> initProjectDataSet(Project project){

        Map<String, Object> result = new HashMap<>();

        //Add a simple project user
        User simpleUser = (User) userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get();
//                builder.given_a_user();
//        builder.addUserToProject(project, simpleUser.getUsername(), READ);

        //Add a project admin
        User admin = builder.given_a_user();
        builder.addUserToProject(project, admin.getUsername(), ADMINISTRATION);

        /*super admin data*/
        //Create an annotation (by superadmin)
        ImageInstance image = builder.given_an_image_instance(project);
        SliceInstance slice = builder.given_a_slice_instance(image, builder.given_an_abstract_slice());
        UserAnnotation annotation = builder.given_a_user_annotation(slice);
        //Create a description
        Description description = builder.given_a_description(annotation);
        //Create a property
        Property property = builder.given_a_property(annotation);
        //Create an attached file
        AttachedFile attachedFile = builder.given_a_attached_file(annotation);
        //Create a tag
        TagDomainAssociation tda = builder.given_a_tag_association(builder.given_a_tag(), annotation);

        result.put("image", image);
        result.put("slice", slice);
        result.put("annotation", annotation);
        result.put("description", description);
        result.put("property", property);
        result.put("attachedFile", attachedFile);
        result.put("tagDomainAssociation", tda);

        /*admin data*/
        //Create an annotation (by admin)
        ImageInstance imageAdmin =builder.given_an_image_instance(project);
        imageAdmin.setUser(admin);
        builder.persistAndReturn(imageAdmin);

        SliceInstance sliceAdmin = builder.given_a_slice_instance(imageAdmin, builder.given_an_abstract_slice());
        UserAnnotation annotationAdmin = builder.given_a_user_annotation(slice);
        annotationAdmin.setUser(admin);
        builder.persistAndReturn(annotationAdmin);
        //Create a description
        Description descriptionAdmin = builder.given_a_description(annotationAdmin);
        //Create a property
        Property propertyAdmin = builder.given_a_property(annotationAdmin);
        //Create an attached file
        AttachedFile attachedFileAdmin = builder.given_a_attached_file(annotationAdmin);
        //Create a tag
        TagDomainAssociation tdaAdmin = builder.given_a_tag_association(builder.given_a_tag(), annotationAdmin);

        result.put("imageAdmin", imageAdmin);
        result.put("sliceAdmin", sliceAdmin);
        result.put("annotationAdmin", annotationAdmin);
        result.put("descriptionAdmin", descriptionAdmin);
        result.put("propertyAdmin", propertyAdmin);
        result.put("attachedFileAdmin", attachedFileAdmin);
        result.put("tagDomainAssociationAdmin", tdaAdmin);

        /*simple user data*/
        //Create an annotation (by user)
        ImageInstance imageUser =builder.given_an_image_instance(project);
        imageUser.setUser(simpleUser);
        builder.persistAndReturn(imageUser);

        SliceInstance sliceUser = builder.given_a_slice_instance(imageUser, builder.given_an_abstract_slice());
        UserAnnotation annotationUser = builder.given_a_user_annotation(slice);
        annotationUser.setUser(simpleUser);
        builder.persistAndReturn(annotationUser);
        //Create a description
        Description descriptionUser = builder.given_a_description(annotationUser);
        //Create a property
        Property propertyUser = builder.given_a_property(annotationUser);
        //Create an attached file
        AttachedFile attachedFileUser = builder.given_a_attached_file(annotationUser);
        //Create a tag
        TagDomainAssociation tdaUser = builder.given_a_tag_association(builder.given_a_tag(), annotationUser);

        result.put("imageUser", imageUser);
        result.put("sliceUser", sliceUser);
        result.put("annotationUser", annotationUser);
        result.put("descriptionUser", descriptionUser);
        result.put("propertyUser", propertyUser);
        result.put("attachedFileUser", attachedFileUser);
        result.put("tagDomainAssociationUser", tdaUser);

        return result;
    }

    // **************
    // OVERRIDE
    // **************

    @Override
    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_add_domain() {
        expectOK (() -> when_i_add_domain());
        // User with no ACL can create an project
    }

    @Override
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_add_domain() {
        expectOK (() -> when_i_add_domain());
        // User with READ permission can create another project
    }

    @Override
    public void when_i_get_domain() {
        projectService.get(project.getId());
    }

    @Override
    protected void when_i_add_domain() {
        projectService.add(BasicInstanceBuilder.given_a_not_persisted_project().toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        projectService.update(project, project.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        Project projectToDelete = project;
        projectService.delete(projectToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.empty();
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.ADMINISTRATION);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.WRITE);
    }

    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }
}

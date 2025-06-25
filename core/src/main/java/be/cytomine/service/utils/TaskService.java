package be.cytomine.service.utils;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.Task;
import be.cytomine.utils.TaskComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Service
@Transactional
public class TaskService {

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    SequenceService sequenceService;

    public Task get(Long id) {
        if (id == null) {
            return null;
        }
        return getFromDatabase(id);
    }

    public List<TaskComment> listLastComments(Project project) {
       securityACLService.check(project,READ);
        List<TaskComment> comments = listFromDatabase(project.getId());
       return comments;
    }

    /**
     * Create a new task
     * @param project Project concerning by this task
     * @param user User that create the task
     * @return Task created
     */
    public Task createNewTask(Project project, User user, boolean printInActivity) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        Task task = new Task();
        task.setProjectIdent(project!=null?project.getId():null);
        task.setUserIdent(user.getId());
        task.setPrintInActivity(printInActivity);
        //task.addToComments("Task started...")
        task = saveOnDatabase(task);
        return task;
    }

    /**
     * Update task status, don't change progress
     * @param task Task to update
     * @param comment Comment for the new task status
     */
    public Task updateTask(Task task, String comment) {
        if (task!=null) {
            User user = userRepository.getById(task.getUserIdent());
            securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        }
        return updateTask(task,(task!=null? task.getProgress() : -1),comment);
    }

    /**
     * Update task status
     * @param task Task to update
     * @param progress New progress value (0-100)
     * @param comment Comment for the new task status
     */
    public Task updateTask(Task task, int progress, String comment) {
        if(task==null) {
            return null;
        }
        User user = userRepository.getById(task.getUserIdent());
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        task.setProgress(progress);
        addComment(task,progress+"%:" + comment);
        task = saveOnDatabase(task);
        return task;
    }

    /**
     * Close task and put progress to 100
     * @param task Task to close
     * @return Closed task
     */
    public Task finishTask(Task task) {
        if(task==null) {
            return null;
        }
        User user = userRepository.getById(task.getUserIdent());
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        task.setProgress(100);
        updateTask(task,100,"Task completed...");
        task = get(task.getId());
        return task;
    }


    public List<String> getLastComments(Task task,int max) {
        //sql request retrieve n last comments for task
        return entityManager.createNativeQuery(
                        "SELECT comment FROM task_comment where task_id = ? order by timestamp desc limit ?")
                .setParameter(1, task.getId())
                .setParameter(2, max).getResultList();
    }

    private Task saveOnDatabase(Task task) {

        if(getFromDatabase(task.getId())==null) {
            task.setId(sequenceService.generateID());
            if (task.getProjectIdent()==null) {
                entityManager.createNativeQuery("INSERT INTO task (id,progress,user_id,print_in_activity) VALUES (?,?,?,?)")
                        .setParameter(1, task.getId())
                        .setParameter(2, task.getProgress())
                        .setParameter(3, task.getUserIdent())
                        .setParameter(4, task.isPrintInActivity())
                        .executeUpdate();
            } else {
                // cannot pass "null" to project parameter
                entityManager.createNativeQuery("INSERT INTO task (id,progress,project_id,user_id,print_in_activity) VALUES (?,?,?,?,?)")
                        .setParameter(1, task.getId())
                        .setParameter(2, task.getProgress())
                        .setParameter(3, task.getProjectIdent())
                        .setParameter(4, task.getUserIdent())
                        .setParameter(5, task.isPrintInActivity())
                        .executeUpdate();
            }
        } else {
            entityManager.createNativeQuery("UPDATE task set progress=? WHERE id=?")
                    .setParameter(1, task.getProgress())
                    .setParameter(2, task.getId())
                    .executeUpdate();
        }
        return getFromDatabase(task.getId());
    }

    private Task getFromDatabase(Long id) {

        if (id == null) {
            return null;
        }

        Object[] row;
        try {
            row = (Object[])entityManager.createNativeQuery(
                            "SELECT id,progress,project_id,user_id FROM task where id = ?")
                    .setParameter(1, id.longValue()).getSingleResult();
        }catch (jakarta.persistence.NoResultException ex) {
            return null;
        }

        Task task = new Task();
        task.setId((Long)row[0]);
        task.setProgress(((Long)row[1]).intValue());
        task.setProjectIdent(row[2]!=null ? (Long)row[2] : null);
        task.setUserIdent((Long)row[3]);
        return task;
    }

    private List<TaskComment> listFromDatabase(Long idProject) {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT comment, timestamp FROM task_comment tc, task t WHERE tc.task_id = t.id  AND project_id = ? and t.print_in_activity=true ORDER BY timestamp DESC")
                .setParameter(1, idProject).getResultList();
        return rows.stream().map(x -> {
            TaskComment comment = new TaskComment();
            comment.setComment((String)x[0]);
            comment.setTimestamp((Long)x[1]);
            return comment;
        }).collect(Collectors.toList());
    }

    private void addComment(Task task,String comment) {
        if(comment!=null && !comment.equals("")) {
            TaskComment taskComment = new TaskComment();
            taskComment.setTaskIdent(task.getId());
            taskComment.setComment(comment);
            taskComment.setTimestamp(new Date().getTime());
            saveCommentOnDatabase(taskComment);
        }
    }

    private void saveCommentOnDatabase(TaskComment comment) {
        entityManager.createNativeQuery("INSERT INTO task_comment (task_id,comment,timestamp) VALUES (?,?,?)")
                .setParameter(1, comment.getTaskIdent())
                .setParameter(2, comment.getComment())
                .setParameter(3, comment.getTimestamp())
                .executeUpdate();
    }
}

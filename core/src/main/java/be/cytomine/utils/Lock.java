package be.cytomine.utils;

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
import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class Lock {

    private Map<Long, ReentrantLock> projectLocks = new ConcurrentHashMap<>();
    private Map<String, ReentrantLock> wsSessionsLock = new ConcurrentHashMap<>();

    private Map<Long, ReentrantLock> customUILocks = new ConcurrentHashMap<>();
    private static Lock lock = null;

    private Lock() {}

    public static Lock getInstance() {
        synchronized (Lock.class) {
            if (lock==null) {
                lock = new Lock();
            }
            return lock;
        }
    }

    public boolean lockProject(Project project) {
        try {
            log.debug("Try to lock project " + project.getId());
            projectLocks.putIfAbsent(project.getId(), new ReentrantLock());
            log.debug("Project {} current lock {}", project.getId(), projectLocks.get(project.getId()).isLocked());
            boolean result = projectLocks.get(project.getId()).tryLock(60, TimeUnit.SECONDS);
            log.debug("Project {} lock result {}", project.getId(), result);
            return result;
        } catch (InterruptedException e) {
            throw new ServerException("Cannot acquire lock on domain " + project, e);
        }
    }

    public void unlockProject(Project project) {
        log.debug("Unlock project " + project.getId());
        projectLocks.get(project.getId()).unlock();
    }

    public boolean lockCustomUI(Project project) {
        try {
            log.debug("Try to lock custom UI for project " + project.getId());
            customUILocks.putIfAbsent(project.getId(), new ReentrantLock());
            log.debug("Custom UI Project {} current lock {}", project.getId(), customUILocks.get(project.getId()).isLocked());
            boolean result = customUILocks.get(project.getId()).tryLock(60, TimeUnit.SECONDS);
            log.debug("Custom UI Project {} lock result {}", project.getId(), result);
            return result;
        } catch (InterruptedException e) {
            throw new ServerException("Cannot acquire lock on domain " + project, e);
        }
    }

    public void unlockCustomUI(Project project) {
        log.debug("Unlock Custom UI  project " + project.getId());
        customUILocks.get(project.getId()).unlock();
    }


    public boolean lockUsedWsSession(String userId) {
        try {
            log.debug("Try to lock web socket session " + userId);
            wsSessionsLock.putIfAbsent(userId, new ReentrantLock());
            log.debug("WebSocket session of user {} current lock {}", userId, wsSessionsLock.get(userId).isLocked());
            boolean result = wsSessionsLock.get(userId).tryLock(60, TimeUnit.SECONDS);
            log.debug("WebSocket session for user {} lock result {}", userId, result);
            return result;
        } catch (InterruptedException e) {
            throw new ServerException("Cannot acquire lock on user " + userId, e);
        }
    }

    public void unlockUserWsSession(String userId) {
        log.debug("Unlock WebSocket session for user " + userId);
        wsSessionsLock.get(userId).unlock();
    }
}

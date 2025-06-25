package be.cytomine.controller.security;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import be.cytomine.CytomineCoreApplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin")
public class GrantRoleTests {

    @Autowired
    private MockMvc restGrandRoleControllerMockMvc;

    protected MockHttpSession session;

    @Test
    @WithMockUser(username = "admin")
    public void open_close_admin_session_as_admin() throws Exception {
        startSession();

        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(false))
                .andExpect(jsonPath("$.userByNow").value(true));

        restGrandRoleControllerMockMvc.perform(get("/session/admin/open.json")
                        .session(session))
                .andExpect(status().isOk());

        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(true))
                .andExpect(jsonPath("$.userByNow").value(true));

        restGrandRoleControllerMockMvc.perform(get("/session/admin/close.json")
                        .session(session))
                .andExpect(status().isOk());

        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(false))
                .andExpect(jsonPath("$.userByNow").value(true));

        endSession();
    }

    protected void startSession() {
        session = new MockHttpSession();
    }

    protected void endSession() {
        session.clearAttributes();
        session = null;
    }
}

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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PageUtilsTests {

    @Test
    public void pagination_from_already_limited_results() {
        Page page = PageUtils.buildPageFromPageResults(new ArrayList<>(List.of("a", "b", "c", "d", "e")), 0L, 0L, 5L);
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(5);

        page = PageUtils.buildPageFromPageResults(new ArrayList<>(List.of("a", "b", "c")), 3L, 0L, 5L);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);

        page = PageUtils.buildPageFromPageResults(new ArrayList<>(List.of("c", "d", "e")), 3L, 2L, 5L);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);

        page = PageUtils.buildPageFromPageResults(new ArrayList<>(List.of()), 3L, 6L, 5L);
        assertThat(page.getContent()).hasSize(0);
        assertThat(page.getTotalElements()).isEqualTo(5);

    }
}

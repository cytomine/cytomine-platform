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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTests {

    @Test
    public void pagination_from_already_limited_results() {
        assertThat(StringUtils.obscurify("password", 0)).isEqualTo("********");
        assertThat(StringUtils.obscurify("password", 1)).isEqualTo("p******d");
        assertThat(StringUtils.obscurify("password", 2)).isEqualTo("pa****rd");
        assertThat(StringUtils.obscurify("password", 3)).isEqualTo("pas**ord");
        assertThat(StringUtils.obscurify("password", 4)).isEqualTo("password");
        assertThat(StringUtils.obscurify("password", 5)).isEqualTo("password");
        assertThat(StringUtils.obscurify("password", 10)).isEqualTo("password");

        assertThat(StringUtils.obscurify("", 0)).isEqualTo("<EMPTY>");
        assertThat(StringUtils.obscurify("", 10)).isEqualTo("<EMPTY>");
    }

    @Test
    public void null_or_empty_parameters_extracted_as_null() {
        assertThat(StringUtils.extractListFromParameter("")).isNull();
        assertThat(StringUtils.extractListFromParameter(null)).isNull();
    }

    @Test
    public void parameters_extracted_as_list_of_long() {
        List<Long> expectedList = new ArrayList<>(Arrays.asList((long)145, (long)146, (long)0, (long)-1));
        assertThat(StringUtils.extractListFromParameter("145,146,0,-1")).isEqualTo(expectedList);
    }

    @Test
    public void get_local_date_as_string() {
        List<Long> expectedList = new ArrayList<>(Arrays.asList((long)145, (long)146, (long)0, (long)-1));
        assertThat(StringUtils.extractListFromParameter("145,146,0,-1")).isEqualTo(expectedList);
    }

    @Test
    public void get_simple_format_local_date_as_string() {
        List<Long> expectedList = new ArrayList<>(Arrays.asList((long)145, (long)146, (long)0, (long)-1));
        assertThat(StringUtils.extractListFromParameter("145,146,0,-1")).isEqualTo(expectedList);
    }

}

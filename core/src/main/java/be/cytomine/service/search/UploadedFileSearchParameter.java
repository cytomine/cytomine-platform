package be.cytomine.service.search;

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

import be.cytomine.utils.filters.SearchParameterEntry;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class UploadedFileSearchParameter {

    SearchParameterEntry storage;

    SearchParameterEntry user;

    SearchParameterEntry originalFilename;

    public Optional<SearchParameterEntry> findStorage() {
        return Optional.ofNullable(storage);
    }

    public Optional<SearchParameterEntry> findUser() {
        return Optional.ofNullable(user);
    }

    public Optional<SearchParameterEntry> findOriginalFilename() {
        return Optional.ofNullable(originalFilename);
    }

    public List<SearchParameterEntry> toList() {
        List<SearchParameterEntry> list = new ArrayList<>();
        findStorage().ifPresent(list::add);
        findUser().ifPresent(list::add);
        findOriginalFilename().ifPresent(list::add);
        return list;
    }
}

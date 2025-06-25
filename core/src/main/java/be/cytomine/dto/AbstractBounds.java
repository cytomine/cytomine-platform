package be.cytomine.dto;

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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import be.cytomine.utils.MinMax;

@Getter
public class AbstractBounds {

    protected <T extends Comparable> void updateMinMax(MinMax<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue.getMin()==null) {
            currentValue.setMin(newValue);
        } else if(newValue.compareTo(currentValue.getMin())<0) {
            currentValue.setMin(newValue);
        }
        if (currentValue.getMax()==null) {
            currentValue.setMax(newValue);
        } else if(newValue.compareTo(currentValue.getMax())>0) {
            currentValue.setMax(newValue);
        }
    }

    protected <T extends Comparable> void updateChoices(MinMax<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue.getList()==null) {
            currentValue.setList(new ArrayList<>());
        }
        if (!currentValue.getList().contains(newValue)) {
            currentValue.getList().add(newValue);
        }
    }

    protected <T extends Comparable> void updateChoices(List<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue==null) {
            currentValue = new ArrayList<>();
        }
        if (!currentValue.contains(newValue)) {
            currentValue.add(newValue);
        }
    }
}


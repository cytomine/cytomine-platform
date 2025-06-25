package be.cytomine.service.image;

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

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.meta.Property;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ImagePropertiesService {

    private static Map<String, Integer> PIXEL_TYPE = Map.of("uint8", 8, "int8", 8, "uint16", 16, "int16", 16);

    @Autowired
    AbstractImageRepository abstractImageRepository;
    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AbstractSliceRepository abstractSliceRepository;

    public void extractUseful(AbstractImage image) throws IOException, IllegalAccessException {
        extractUseful(image, false);
    }

    public void extractUseful(AbstractImage image, boolean deep) throws IOException, IllegalAccessException {

        try {
            Map<String, Object> properties = imageServerService.properties(image);
            Map<String, Object> imageProperties = (Map<String, Object>)properties.getOrDefault("image", Map.of());
            JsonObject imagePropertiesObject = new JsonObject(imageProperties);


            image.setWidth(imagePropertiesObject.getJSONAttrInteger("width"));
            image.setHeight(imagePropertiesObject.getJSONAttrInteger("height"));
            image.setDepth(imagePropertiesObject.getJSONAttrInteger("depth"));
            image.setDuration(imagePropertiesObject.getJSONAttrInteger("duration"));

            image.setChannels(imagePropertiesObject.getJSONAttrInteger("n_concrete_channels"));
            image.setSamplePerPixel(imagePropertiesObject.getJSONAttrInteger("n_samples"));

            image.setPhysicalSizeX(imagePropertiesObject.getJSONAttrDouble("physical_size_x"));
            image.setPhysicalSizeY(imagePropertiesObject.getJSONAttrDouble("physical_size_y"));
            image.setPhysicalSizeZ(imagePropertiesObject.getJSONAttrDouble("physical_size_z"));

            image.setFps(imagePropertiesObject.getJSONAttrDouble("frame_rate"));

            Map<String, Object> instrument = ((Map<String, Object>)properties.getOrDefault("instrument", Map.of()));
            Map<String, Object> objective = ((Map<String, Object>)instrument.getOrDefault("objective", Map.of()));
            Integer nominal_magnification = (Integer)objective.get("nominal_magnification");

            image.setMagnification(nominal_magnification);
            image.setBitPerSample(imagePropertiesObject.getJSONAttrInteger("bits", 8));
            image.setTileSize(256);  // [PIMS] At this stage, we only support normalized-tiles.

            abstractImageRepository.save(image);

            if (deep) {
                List<Map<String, Object>> channels = (List<Map<String, Object>>)properties.getOrDefault("channels", List.of());
                for (int i = 0; i < image.getApparentChannels(); i += image.getSamplePerPixel()) {
                    Map<String, Object> channel = (Map<String, Object>) channels.get(i);
                    int index = Math.floorDiv((Integer)channel.get("index"), image.getSamplePerPixel());
                    String name = (String) channel.get("suggested_name");
                    String color = (String) channel.get("color");
                    if (image.getSamplePerPixel() != 1) {
                        color = null;

                        List<String> parts = new ArrayList<>();
                        for (int j = 0; j<image.getSamplePerPixel() ; j++) {
                            parts.add((String) channels.get(i+j).get("suggested_name"));
                        }
                        name = parts.stream().distinct().collect(Collectors.joining("|"));
                    }
                    List<AbstractSlice> slices = abstractSliceRepository.findAllByImageAndChannel(image, index)
                            .stream().filter(x -> Objects.equals(x.getChannel(), (Integer)channel.get("index"))).toList();
                    for (AbstractSlice slice : slices) {
                        slice.setChannelName(name);
                        slice.setChannelColor(color);
                    }
                    abstractSliceRepository.saveAll(slices);
                }
            }
        } catch (Exception e) {
            log.error("Error when extracting metadata", e);
        }
    }

    public void regenerate(AbstractImage image, boolean deep) throws IOException, IllegalAccessException {
        extractUseful(image, deep);
    }
}

@Getter
@Setter
@AllArgsConstructor
class ImagePropertiesValue {
    String key;
    String name;
    Function parser;
}

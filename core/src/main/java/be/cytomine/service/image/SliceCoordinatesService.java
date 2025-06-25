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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.dto.image.SliceCoordinate;
import be.cytomine.dto.image.SliceCoordinates;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.image.SliceInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SliceCoordinatesService {

    @Autowired
    AbstractSliceRepository abstractSliceRepository;

    @Autowired
    SliceInstanceRepository sliceInstanceRepository;

    public SliceCoordinates getSliceCoordinates(AbstractImage image) {
        List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(image);
        SliceCoordinates sliceCoordinates = new SliceCoordinates(
                slices.stream().map(AbstractSlice::getChannel).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getZStack).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getTime).distinct().sorted().collect(Collectors.toList())
        );
        return sliceCoordinates;
    }


    public SliceCoordinate getReferenceSliceCoordinate(AbstractImage image) {
        SliceCoordinates sliceCoordinates = getSliceCoordinates(image);
        if (sliceCoordinates.getChannels().isEmpty()) {
            throw new WrongArgumentException("Cannot retrieve reference slices for AbstractImage " + image.getId());
        }
        SliceCoordinate referenceSliceCoordinates = new SliceCoordinate(
                sliceCoordinates.getChannels().get((int) Math.floor(sliceCoordinates.getChannels().size()/2)),
                sliceCoordinates.getZStacks().get((int) Math.floor(sliceCoordinates.getZStacks().size()/2)),
                sliceCoordinates.getTimes().get((int) Math.floor(sliceCoordinates.getTimes().size()/2))
        );
        return referenceSliceCoordinates;
    }

    public AbstractSlice getReferenceSlice(AbstractImage abstractImage) {
        SliceCoordinate sliceCoordinate = getReferenceSliceCoordinate(abstractImage);
        return abstractSliceRepository.findByImageAndChannelAndZStackAndTime(abstractImage, sliceCoordinate.getChannel(), sliceCoordinate.getZStack(), sliceCoordinate.getTime())
                .orElseThrow(() -> new ObjectNotFoundException("AbstractSlice", "image:" + abstractImage.getId() + "," + sliceCoordinate.getChannel()+ ":" + sliceCoordinate.getZStack() + ":" + sliceCoordinate.getTime()));
    }

    public SliceInstance getReferenceSlice(ImageInstance imageInstance) {
        AbstractSlice abstractSlice = getReferenceSlice(imageInstance.getBaseImage());
        return sliceInstanceRepository.findByBaseSliceAndImage(abstractSlice, imageInstance).orElse(null);
    }

    public SliceInstance getReferenceSliceOptimized(ImageInstance imageInstance) {
        List<SliceInstance> slicesInstances = sliceInstanceRepository.findAllByImage(imageInstance);
        SliceCoordinates sliceCoordinates = new SliceCoordinates(
                slicesInstances.stream().map(x -> x.getBaseSlice().getChannel()).distinct().sorted().collect(Collectors.toList()),
                slicesInstances.stream().map(x -> x.getBaseSlice().getZStack()).distinct().sorted().collect(Collectors.toList()),
                slicesInstances.stream().map(x -> x.getBaseSlice().getTime()).distinct().sorted().collect(Collectors.toList())
        );
        if (sliceCoordinates.getChannels().isEmpty()) {
            throw new WrongArgumentException("Cannot retrieve reference slices for AbstractImage " + imageInstance.getBaseImage().getId());
        }
        SliceCoordinate referenceSliceCoordinates = new SliceCoordinate(
                sliceCoordinates.getChannels().get((int) Math.floor(sliceCoordinates.getChannels().size()/2)),
                sliceCoordinates.getZStacks().get((int) Math.floor(sliceCoordinates.getZStacks().size()/2)),
                sliceCoordinates.getTimes().get((int) Math.floor(sliceCoordinates.getTimes().size()/2))
        );
        SliceInstance result = slicesInstances.stream().filter(x -> x.getImage().getId().equals(imageInstance.getId()) && x.getBaseSlice().getChannel().equals(referenceSliceCoordinates.getChannel()) && x.getBaseSlice().getZStack().equals(referenceSliceCoordinates.getZStack()) && x.getBaseSlice().getTime().equals(referenceSliceCoordinates.getTime()))
                .findFirst().orElseThrow(() -> new ObjectNotFoundException("AbstractSlice", "image:" + imageInstance.getBaseImage().getId() + "," + referenceSliceCoordinates.getChannel()+ ":" + referenceSliceCoordinates.getZStack() + ":" + referenceSliceCoordinates.getTime()));
        return result;
    }
}

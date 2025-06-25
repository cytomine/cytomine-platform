package be.cytomine.domain.image;

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

import java.util.Arrays;
import java.util.Optional;

public enum UploadedFileStatus {
    /**
     * Even codes lower than 100 => information
     * Even codes greater or equal to 100 => success
     * Odd codes => error
     */
    UPLOADED (0),

    DETECTING_FORMAT (10),
    ERROR_FORMAT (11), // 3

    EXTRACTING_DATA (20),
    ERROR_EXTRACTION (21),

    CONVERTING (30),
    ERROR_CONVERSION (31), // 4

    DEPLOYING (40),
    ERROR_DEPLOYMENT (41), // 8

    UNPACKING (50),
    ERROR_UNPACKING (51),

    CHECKING_INTEGRITY (60),
    ERROR_INTEGRITY (61),
    DEPLOYED (100),
    EXTRACTED (102),
    CONVERTED (104),

    UNPACKED (106);

    private final int code;

    UploadedFileStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static Optional<UploadedFileStatus> findByCode(int code) {
        return Arrays.stream(values()).filter(x -> x.getCode() == code).findFirst();
    }
}

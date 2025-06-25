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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.*;

@Converter
public class LongArrayToBytesConverter implements AttributeConverter<Long[],byte[]>{
    @Override
    public byte[] convertToDatabaseColumn(Long[] attribute) {
        if (attribute==null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(attribute);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("cannot serialize", e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    @Override
    public Long[] convertToEntityAttribute(byte[] dbData) {
        if (dbData==null) {
            return null;
        }
        try (ByteArrayInputStream b = new ByteArrayInputStream(dbData)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                Long[] data = (Long[]) o.readObject();
                return data;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("cannot deserialize", e);
        }

    }
}

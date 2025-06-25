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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class LTreeType implements UserType<String> {
    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String s, String j1) {
        return s!= null && s.equals(j1);
    }

    @Override
    public int hashCode(String s) {
        return s.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet resultSet, int i, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws SQLException {
        return resultSet.getString(i);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, String s, int i, SharedSessionContractImplementor sharedSessionContractImplementor) throws SQLException {
        preparedStatement.setObject(i, s, Types.OTHER);
    }

    @Override
    public String deepCopy(String s) {
        if(s == null){
            return null;
        }
        return new String(s);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String s) {
        return s;
    }

    @Override
    public String assemble(Serializable serializable, Object o) {
        return (String) serializable;
    }

    @Override
    public String replace(String s, String j1, Object o) {
        return deepCopy(s);
    }
}

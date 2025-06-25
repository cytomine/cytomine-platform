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

import org.hibernate.proxy.HibernateProxy;
public class ClassUtils {

    /**
     * Get the class name of an object without package name
     * @param o Object
     * @return Class name (without package) of o
     */
    public static String getClassName(Object o) {
        String name = o.getClass().getName();   //be.cytomine.image.Image
        int exeed = name.indexOf("_\\$\\$_javassist");
        if (exeed!=-1) {
            name = name.substring(0,exeed); //if ex be.cytomine.image.Image_$$_javassistxxxx...remove all after  _$$
        } else if (o instanceof HibernateProxy hibernateProxy) {
            name = hibernateProxy.getClass().getSuperclass().getName(); // if ex be.cytomine.image.Image$HibernateProxy$GfTtnrQ6, take the super class (Image)
        }
        String[] array = name.split("\\.") ; //[be,cytomine,image,Image]
        //log.info array.length
        return array[array.length - 1]; // Image
    }
}

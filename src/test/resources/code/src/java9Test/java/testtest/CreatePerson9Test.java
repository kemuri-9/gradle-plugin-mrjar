/**
 * Copyright 2021 Steven Walters
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package testtest;
 
import java.net.URL;
import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.*;

class CreatePerson9Test {

    @Test
    void testCreate() {
        Person person = CreatePerson.randomPerson();
        Assertions.assertNotNull(person);
        Assertions.assertNotNull(person.name);
        /* when module patching, the patch takes priority. so the java 8 version will be used.
         * as such, it is better to actually create the Jar and utilize it instead of class files */
        boolean java8Version = getClass().getModule().isNamed();
        URL classSource = getClass().getClassLoader().getResource(CreatePerson.class.getName().replace(".", "/") + ".class");
        java8Version &= !classSource.getProtocol().equalsIgnoreCase("jar");
        Assertions.assertEquals(java8Version ? 5 : 10, person.name.length());
        Assertions.assertNotNull(person.birthDate);
        int endDate = java8Version ? 6 : 11;
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, endDate)) >= 0);
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, endDate+5)) <= 0);
    }
}

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
 
import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.*;
 
class CreatePersonTest {

    @Test
    void testCreate() {
        Person person = CreatePerson.randomPerson();
        Assertions.assertNotNull(person);
        Assertions.assertNotNull(person.name);
        Assertions.assertTrue(person.name.length() >= 5);
        Assertions.assertTrue(person.name.length() <= 15);
        Assertions.assertNotNull(person.birthDate);
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, 06)) >= 0);
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, 21)) <= 0);
    }
}

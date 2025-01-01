/**
 * Copyright 2021-2025 Steven Walters
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
 
class CreatePerson10Test {

    @Test
    void testCreate() {
        Person person = CreatePerson.randomPerson();
        Assertions.assertNotNull(person);
        Assertions.assertNotNull(person.name);
        Assertions.assertEquals(15, person.name.length());
        Assertions.assertNotNull(person.birthDate);
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, 16)) >= 0);
        Assertions.assertTrue(person.birthDate.compareTo(LocalDate.of(2021, 7, 21)) <= 0);
    }
}

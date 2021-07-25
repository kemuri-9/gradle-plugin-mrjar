/*
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
package testtest
 
import java.time.LocalDate
 
import spock.lang.Specification

import test.*
 
class CreatePerson9Spec extends Specification {

    void 'test create'() {
        expect:
        Person person = CreatePerson.randomPerson()
        person != null
        person.name != null
        person.name.length() == 10
        person.birthDate != null
        person.birthDate >= LocalDate.of(2021, 7, 11)
        person.birthDate <= LocalDate.of(2021, 7, 16)
    }
}

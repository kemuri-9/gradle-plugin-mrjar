/*
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
package test

import java.time.LocalDate

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils

class CreatePerson {

    public static Person randomPerson() {
        String name = RandomStringUtils.random(15)
        LocalDate date = LocalDate.of(2021, 7, 1).plusDays(RandomUtils.nextInt(15, 20))
        new Person(name, date)
    }
}

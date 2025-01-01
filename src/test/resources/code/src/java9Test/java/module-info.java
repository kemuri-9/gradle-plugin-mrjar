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
/* multi line comment
 */  /* with another starting on the same line
 */  /* with another starting on the same line */

/**
 * Testing module of Test
 */
open module test/* comment */
    .test // comment
    {
    requires java.base;
    requires transitive test;
    requires org.junit.jupiter.api;

    exports testtest;
}

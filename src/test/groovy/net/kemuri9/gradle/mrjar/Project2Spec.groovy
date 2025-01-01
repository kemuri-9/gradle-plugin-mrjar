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
package net.kemuri9.gradle.mrjar

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class Project2Spec extends GradleRunnerSpecification {

    static final File projectDir = new File('testprojects/test2')

    void 'tasks'() {
        expect:
        BuildResult result = newGradleRunner('tasks').withProjectDir(projectDir).withGradleVersion(gradleVersion).build()
        String output = result.output
        checkTaskListContainsVersion(output, 9, true)
        checkTaskListContainsGroovydoc(output, 9)
        checkTaskListContainsVersion(output, 10, true)
        checkTaskListContainsGroovydoc(output, 10)

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    void 'list artifacts'() {
        expect:
        BuildResult result = newGradleRunner('listArtifacts').withProjectDir(projectDir).withGradleVersion(gradleVersion).build()
        String output = result.output
        // base
        output.contains('test2.jar')
        output.contains('test2-sources.jar')
        output.contains('test2-groovydoc.jar')
        // java 9
        output.contains('test2-java9-sources.jar')
        output.contains('test2-java9-groovydoc.jar')
        // java 10
        output.contains('test2-java10-sources.jar')
        output.contains('test2-java10-groovydoc.jar')

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    void 'build'() {
        setup:
        Path outputDir = new File(projectDir, 'build/libs').toPath()
        Path codeDir = Paths.get('src/test/resources/code')
        Path buildJarPath = outputDir.resolve('test2.jar')
        Path sourceJarPath = outputDir.resolve('test2-sources.jar')
        Path source9JarPath = outputDir.resolve('test2-java9-sources.jar')
        Path source10JarPath = outputDir.resolve('test2-java10-sources.jar')
        Path groovydoc9JarPath = outputDir.resolve('test2-java9-groovydoc.jar')
        Path groovydoc10JarPath = outputDir.resolve('test2-java10-groovydoc.jar')

        expect:
        BuildResult result = newGradleRunner('clean', 'build', 'check')
                .withProjectDir(projectDir).withGradleVersion(gradleVersion).build()
        result.tasks(TaskOutcome.FAILED).empty

        result.output.contains(':test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')
        result.output.contains(':java9Test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')
        result.output.contains(':java10Test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')

        List<String> buildJar = jarPaths(buildJarPath)
        buildJar == ['META-INF/MANIFEST.MF', 'META-INF/versions/10/test/CreatePerson.class',
            'META-INF/versions/9/test/CreatePerson.class',
            'test/CreatePerson.class', 'test/Person.class']
        String manifest = new String(jarFileContents(buildJarPath, 'META-INF/MANIFEST.MF'), StandardCharsets.US_ASCII)
        manifest.contains('Multi-Release: true')

        List<String> sourceJar = jarPaths(sourceJarPath)
        sourceJar == ['META-INF/MANIFEST.MF', 'test/CreatePerson.groovy', 'test/Person.groovy']
        byte[] create8 = jarFileContents(sourceJarPath, 'test/CreatePerson.groovy')
        create8 == Files.readAllBytes(codeDir.resolve('src/main/groovy/test/CreatePerson.groovy'))

        List<String> source9Jar = jarPaths(source9JarPath)
        source9Jar == ['META-INF/MANIFEST.MF', 'test/CreatePerson.groovy', 'test/Person.groovy']
        byte[] create9 = jarFileContents(source9JarPath, 'test/CreatePerson.groovy')
        create9 == Files.readAllBytes(codeDir.resolve('src/java9/groovy/test/CreatePerson.groovy'))

        List<String> source10Jar = jarPaths(source10JarPath)
        source10Jar == ['META-INF/MANIFEST.MF', 'test/CreatePerson.groovy', 'test/Person.groovy']
        byte[] create10 = jarFileContents(source10JarPath, 'test/CreatePerson.groovy')
        create10 == Files.readAllBytes(codeDir.resolve('src/java10/groovy/test/CreatePerson.groovy'))

        List<String> groovydoc9Jar = jarPaths(groovydoc9JarPath)
        groovydoc9Jar.contains('index.html')

        List<String> groovydoc10Jar = jarPaths(groovydoc10JarPath)
        groovydoc10Jar.contains('index.html')

        where:
        gradleVersion << GRADLE_VERSIONS
    }
}

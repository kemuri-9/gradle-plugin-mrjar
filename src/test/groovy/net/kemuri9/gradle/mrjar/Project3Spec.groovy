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
package net.kemuri9.gradle.mrjar

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class Project3Spec extends GradleRunnerSpecification {

    static final File projectDir = new File('testprojects/test3')

    void 'tasks'() {
        expect:
        BuildResult result = GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
            .withArguments('tasks').build()
        String output = result.output
        checkTaskListContainsVersion(output, 9, true, false)
        checkTaskListContainsJavadoc(output, 9)
        checkTaskListContainsVersion(output, 10, true, false)
        checkTaskListContainsJavadoc(output, 10)
    }

    void 'list artifacts'() {
        expect:
        BuildResult result = GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
            .withArguments('listArtifacts').build()
        String output = result.output
        // base
        output.contains('test3.jar')
        output.contains('test3-sources.jar')
        output.contains('test3-javadoc.jar')
        // java 9
        //output.contains('test3-java9-sources.jar')
        output.contains('test3-java9-javadoc.jar')
        // java 10
        //output.contains('test3-java10-sources.jar')
        output.contains('test3-java10-javadoc.jar')
    }

    void 'build'() {
        setup:
        Path outputDir = new File(projectDir, 'build/libs').toPath()
        Path codeDir = Paths.get('src/test/resources/code')
        Path buildJarPath = outputDir.resolve('test3.jar')
        Path sourceJarPath = outputDir.resolve('test3-sources.jar')
        Path javadocJarPath = outputDir.resolve('test3-javadoc.jar')
        Path javadoc9JarPath = outputDir.resolve('test3-java9-javadoc.jar')
        Path javadoc10JarPath = outputDir.resolve('test3-java10-javadoc.jar')

        expect:
        BuildResult result = GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
            .withArguments('clean', 'build', 'check', '--stacktrace', '--continue').build()
        result.tasks(TaskOutcome.FAILED).empty

        result.output.contains(':test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')
        result.output.contains(':java9Test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')
        result.output.contains(':java9Test2 results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped')
        result.output.contains(':java10Test results: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)')

        List<String> buildJar = jarPaths(buildJarPath)
        buildJar == ['META-INF/MANIFEST.MF', 'META-INF/versions/10/test/CreatePerson.class',
            'META-INF/versions/9/test/CreatePerson.class',
            'module-info.class', 'test/CreatePerson.class', 'test/Person.class']
        String manifest = new String(jarFileContents(buildJarPath, 'META-INF/MANIFEST.MF'), StandardCharsets.US_ASCII)
        manifest.contains('Multi-Release: true')

        List<String> sourceJar = jarPaths(sourceJarPath)
        sourceJar == ['META-INF/MANIFEST.MF', 'META-INF/versions/10/test/CreatePerson.java',
            'META-INF/versions/9/test/CreatePerson.java', 'module-info.java',
            'test/CreatePerson.java', 'test/Person.java']
        byte[] create8 = jarFileContents(sourceJarPath, 'test/CreatePerson.java')
        create8 == Files.readAllBytes(codeDir.resolve('src/main/java/test/CreatePerson.java'))

        byte[] create9 = jarFileContents(sourceJarPath, 'META-INF/versions/9/test/CreatePerson.java')
        create9 == Files.readAllBytes(codeDir.resolve('src/java9/java/test/CreatePerson.java'))
        byte[] create10 = jarFileContents(sourceJarPath, 'META-INF/versions/10/test/CreatePerson.java')
        create10 == Files.readAllBytes(codeDir.resolve('src/java10/java/test/CreatePerson.java'))

        List<String> javadocJar = jarPaths(javadocJarPath)
        javadocJar.contains('index.html')
        !javadocJar.contains('module-search-index.js')

        List<String> javadoc9Jar = jarPaths(javadoc9JarPath)
        javadoc9Jar.contains('index.html')
        javadoc9Jar.contains('module-search-index.js')

        List<String> javadoc10Jar = jarPaths(javadoc10JarPath)
        javadoc10Jar.contains('index.html')
        javadoc10Jar.contains('module-search-index.js')
    }
}

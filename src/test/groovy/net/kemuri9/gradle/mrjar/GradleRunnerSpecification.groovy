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

import org.gradle.testkit.runner.GradleRunner

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

abstract class GradleRunnerSpecification extends spock.lang.Specification {

    protected static List<String> GRADLE_VERSIONS = ['8.6', '8.9', '8.12']

    /* gradle test kit does not read gradle home properties when launching,
     * so it has to be manually read and passed along. this is required for some
     * environments where the jdks are manually configured. */
    @spock.lang.Shared
    protected List<String> gradleHomeArgs

    protected void setupSpec() {
        Properties gradleHomeProps = new Properties()
        try {
            Path gradleHome = Paths.get(System.getProperty('test.gradle.user.home', ''))
            gradleHomeProps.load(Files.newBufferedReader(gradleHome.resolve('gradle.properties'), StandardCharsets.UTF_8))
        } catch (IOException ignored) {
            System.err.println(ignored)
        }
        gradleHomeArgs = gradleHomeProps.collect{ k, v -> "-P${k}=${v}".toString() }
    }

    protected void checkIsFile(Path path) {
        assert Files.exists(path)
        assert Files.isRegularFile(path)
    }

    protected void checkTaskListContainsGroovydoc(String output, int version) {
        assert output.contains("java${version}GroovydocJar - Assembles a jar archive containing the groovydoc of the 'java${version}' feature.")
        assert output.contains("java${version}Groovydoc - Generates Groovydoc API documentation for the java${version} source code.")
    }

    protected void checkTaskListContainsJavadoc(String output, int version) {
        assert output.contains("java${version}JavadocJar - Assembles a jar archive containing the javadoc of the 'java${version}' feature.")
        assert output.contains("java${version}Javadoc - Generates Javadoc API documentation for the java${version} source code.")
    }

    protected void checkTaskListContainsVersion(String output, int version, boolean tests, boolean sourceJar = true) {
        assert output.contains("java${version}Classes - Assembles java${version} classes.")
        if (sourceJar) {
            assert output.contains("java${version}SourcesJar - Assembles a jar archive containing the sources of the 'java${version}' feature.")
        }
        assert output.contains("java${version}TestClasses - Assembles java${version} test classes.")
        if (tests) {
            assert output.contains("java${version}Test - Runs the unit tests for java ${version}.")
        }
    }

    protected byte[] jarFileContents(File file, String entryPath) {
        jarFileContents(file.toPath(), entryPath)
    }

    protected byte[] jarFileContents(Path path, String childpath) {
        checkIsFile(path)
        FileSystem zipFs = FileSystems.newFileSystem(path, null)
        Path zipPath = zipFs.getPath('/')
        zipPath = zipPath.resolve(childpath)
        checkIsFile(zipPath)
        byte[] contents = Files.readAllBytes(zipPath)
        zipFs.close()
        contents
    }

    protected List<String> jarPaths(File file, String childpath = null) {
        jarPaths(file.toPath(), childpath)
    }

    protected List<String> jarPaths(Path path, String childpath = null) {
        Path jar = childpath ? path.resolve(childpath) : path
        checkIsFile(jar)
        FileSystem zipFs = FileSystems.newFileSystem(jar, null)
        List<AutoCloseable> closeMe = [zipFs]
        List<String> paths = []
        try {
            for (Path root : zipFs.rootDirectories) {
                Stream<Path> contents = Files.walk(root)
                closeMe.add(0, contents)
                contents.filter(Files.&isRegularFile).forEach{ paths.add(root.relativize(it).toString()) }

            }
        } finally {
            closeMe.each{ it.close() }
        }
        paths.sort(true)
    }

    protected GradleRunner newGradleRunner(String... args) {
        List<String> argList = new ArrayList<>()
        argList.addAll(gradleHomeArgs)
        argList.addAll(args)
        GradleRunner.create().withPluginClasspath().withArguments(argList)
    }
}

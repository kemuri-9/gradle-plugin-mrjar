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
package net.kemuri9.gradle.mrjar;

import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

/**
 * Version details shared between base and added versions
 */
abstract class VersionCommon implements Version {

    protected final ObjectFactory factory;

    protected final Property<Boolean> primaryModuleDefinition;
    final Project project;
    protected final Property<Boolean> useJarInTests;
    protected final Property<Boolean> useToolchain;

    VersionSourceImpl main;
    VersionSourceImpl test;

    @javax.inject.Inject
    protected VersionCommon(Project project, ObjectFactory factory) {
        this.factory = factory;
        this.project = project;
        this.useJarInTests = factory.property(Boolean.class);
        this.useToolchain = factory.property(Boolean.class);
        this.primaryModuleDefinition = factory.property(Boolean.class);
    }

    abstract boolean getIncludesBaseTests();

    boolean getIsUseJarInTests() {
        return Utils.getIsProperty(useJarInTests, Utils.getExtension(project).getUseJarInTests(), false);
    }

    boolean getIsUseToolchain() {
        if (!JavaVersion.current().isCompatibleWith(getVersion())) {
            // current JVM does not support target version, must use a tool chain then
            return true;
        }

        return Utils.getIsProperty(useToolchain, Utils.getExtension(project).getUseToolchain(), false);
    }

    @Override
    public Property<Boolean> getPrimaryModuleDefinition() {
        return primaryModuleDefinition;
    }

    @Override
    public Property<Boolean> getUseJarInTests() {
        return useJarInTests;
    }

    @Override
    public Property<Boolean> getUseToolchain() {
        return useToolchain;
    }

    abstract String getTestName();

    @Override
    public void main(Action<VersionSource> configure) {
        Utils.notNull(configure, "configure");
        if (main == null) {
            main = factory.newInstance(VersionSourceImpl.class, this, "");
        }
        configure.execute(main);
    }

    void postConfigure() {
        if (main != null) {
            main.postConfigure();
        }
        if (test != null) {
            test.postConfigure();
        }

        registerTestIfApplicable();
        Test versionTest = (Test) project.getTasks().findByName(getTestName());
        if (versionTest == null) {
            // no test to configure, skip all further processing
            return;
        }

        if (getIsUseToolchain()) {
            // use toolchain
            Utils.setLauncher(versionTest, Utils.getToolchains(project), getVersion());
        }

        if (getIsUseJarInTests()) {
            // adjust the classpath to remove all src source sets
            FileCollection testClasspath = versionTest.getClasspath();

            SourceSetContainer sourceSets = Utils.getSourceSets(project);
            List<FileCollection> srcOutputs = sourceSets.stream().filter(set -> !Utils.isTestSource(set))
                    .map(set -> set.getOutput().getClassesDirs()).collect(Collectors.toList());
            testClasspath = Utils.fileCollectionMinus(testClasspath, srcOutputs);

            // add the jar in the place of the removed source sets
            Jar jar = (Jar) project.getTasks().getByName("jar");
            testClasspath = testClasspath.plus(project.files(jar.getArchiveFile().get().getAsFile().getAbsoluteFile()));
            versionTest.dependsOn(jar);
            versionTest.setClasspath(testClasspath);
        }
    }

    protected void registerTest(SourceSet set) {}

    protected void registerTestIfApplicable() {};

    @Override
    public void test(Action<VersionSource> configure) {
        Utils.notNull(configure, "configure");
        if (test == null) {
            test = factory.newInstance(VersionSourceImpl.class, this, "Test");
            // register that there is test code to execute for
            registerTest(test.sourceSet);
        }
        configure.execute(test);
    }
}

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
package net.kemuri9.gradle.mrjar;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

/**
 * Version details for Versions that are added to the base version
 */
class VersionAdd extends VersionCommon {

    private final JavaVersion javaVersion;
    private final Property<Boolean> includeBaseTests;

    @javax.inject.Inject
    public VersionAdd(Project project, JavaVersion javaVersion, ObjectFactory factory) {
        super(project, factory);
        this.includeBaseTests = factory.property(Boolean.class);
        this.javaVersion = javaVersion;
    }

    @Override
    public Property<Boolean> getIncludeBaseTests() {
        return includeBaseTests;
    }

    @Override
    boolean getIncludesBaseTests() {
        return Utils.getIsProperty(includeBaseTests, Utils.getExtension(project).getIncludeBaseTests(), false);
    }

    @Override
    String getTestName() {
        return "java" + javaVersion.getMajorVersion() + "Test";
    }

    @Override
    public JavaVersion getVersion() {
        return javaVersion;
    }

    @Override
    protected void registerTest(SourceSet set) {
        final Provider<Test> testTask = project.getTasks().register(getTestName(), Test.class, task -> {
            task.setDescription("Runs the unit tests for java " + javaVersion.getMajorVersion() + ".");
            task.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
            task.getConventionMapping().map("testClassesDirs", ()-> set.getOutput().getClassesDirs());
            task.getConventionMapping().map("classpath", ()-> set.getRuntimeClasspath());
        });
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME, task -> task.dependsOn(testTask));
    }

    @Override
    protected void registerTestIfApplicable() {
        SourceSetContainer sourceSets = Utils.getSourceSets(project);
        if (getIncludesBaseTests()) {
            Test baseTest = (Test) project.getTasks().getByName("test");
            if (test == null) {
                // there is no test code, so the test task needs to be created based on the "main" test code.
                registerTest(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME));
            }

            Test versionTest = (Test) project.getTasks().getByName(getTestName());
            // add the "main" test code to the test classes dirs, if the test is not based on main already
            if (test != null) {
                FileCollection testClassesDirs = Utils.fileCollectionAdd(versionTest.getTestClassesDirs(), baseTest.getTestClassesDirs());
                versionTest.setTestClassesDirs(testClassesDirs);
            }
        }
    }
}

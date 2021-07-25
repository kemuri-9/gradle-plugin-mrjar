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

import static org.gradle.api.attributes.DocsType.SOURCES;
import static org.gradle.api.plugins.internal.JvmPluginsHelper.configureDocumentationVariantWithArtifact;
import static org.gradle.api.plugins.internal.JvmPluginsHelper.findJavaComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaToolchainService;

import net.kemuri9.gradle.mrjar.languages.LanguageSupport;

class VersionSourceImpl extends AbstractValidatingNamedDomainObjectContainer<VersionSourceLanguage> implements VersionSource {

    private static void extendConfiguration(ConfigurationContainer configs, Collection<LanguageSupport> langs,
            SourceSet source, List<SourceSet> targets) {

        Map<String, String> sourceNames = getConfigNames(langs, source);

        for (SourceSet target : targets) {
            Map<String, String> targetNames = getConfigNames(langs, target);
            Set<String> configNames = new HashSet<>(sourceNames.keySet());
            configNames.retainAll(targetNames.keySet());
            for (String configName : configNames) {
                Configuration sourceConfig = null;
                Configuration targetConfig = null;
                try {
                    sourceConfig = configs.getByName(sourceNames.get(configName));
                    targetConfig = configs.getByName(targetNames.get(configName));
                } catch (UnknownConfigurationException ex) {
                    // the configuration is unknown in either source or target, so skip the extension
                    continue;
                }
                sourceConfig.extendsFrom(targetConfig);
            }
        }
    }

    private static Map<String, String> getConfigNames(Collection<LanguageSupport> langs, SourceSet source) {
        Map<String, String> sourceNames = new HashMap<>();
        for (LanguageSupport lang : langs) {
            sourceNames.putAll(lang.getConfigurationNames(source));
        }
        return sourceNames;
    }

    final SourceSet sourceSet;
    private final ObjectFactory objFactory;
    private final JavaToolchainService javaToolchains;
    private final Property<String> patchesModule;
    final VersionCommon version;
    private final Map<JavaVersion, VersionDependencyImpl> dependsOn;
    private boolean hasSourceJar;

    @javax.inject.Inject
    public VersionSourceImpl(VersionCommon version, String name, ObjectFactory objFactory,
            JavaToolchainService javaToolchains) {
        super(VersionSourceLanguage.class, Utils.instantiateFromFactory(objFactory),
                CollectionCallbackActionDecorator.NOOP);
        this.version = version;
        this.objFactory = objFactory;
        this.javaToolchains = javaToolchains;
        this.patchesModule = objFactory.property(String.class);

        Project project = getProject();
        SourceSetContainer sourceSets = Utils.getSourceSets(project);
        if (version instanceof VersionBase) {
            // base version
            name = SourceSet.TEST_SOURCE_SET_NAME.equalsIgnoreCase(name)
                    ? SourceSet.TEST_SOURCE_SET_NAME : SourceSet.MAIN_SOURCE_SET_NAME;
            sourceSet = sourceSets.getByName(name);
        } else {
            // added version
            sourceSet = sourceSets.create("java" + version.getVersion().getMajorVersion() + name);
            // add to the java plugin convention source sets as this is used by some of the other plugins
            project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().add(sourceSet);
        }

        this.dependsOn = new EnumMap<>(JavaVersion.class);
    }

    private void addDependentSources(List<SourceSet> dependSources) {
        Project project = getProject();
        Map<String, LanguageSupport> langs = Utils.getExtension(project).languages;
        // for every language of this source set, handle dependencies to the target
        for (String usedLangName : Utils.filterLanguages(project, sourceSet, langs.keySet())) {
            /* as not every language that is included may be configured in the project,
             * account for that by checking if the language is included in the source set */
            LanguageSupport usedLang = langs.get(usedLangName);
            VersionSourceLanguage configuredLang = this.getNames().contains(usedLangName)
                    ? this.getByName(usedLangName) : null;
            addDependentSources(usedLang, configuredLang, dependSources);
        }
    }

    private <AC extends AbstractCompile, DOC extends SourceTask> void addDependentSources(LanguageSupport lang,
            VersionSourceLanguage configuredLang, List<SourceSet> dependSources) {
        Project project = getProject();
        LanguageSupport.CompileHandler<AC> compileHandler = Utils.cast(lang.getCompilerHandler());
        // if there is documentation configured for the source then also add the sources to it
        LanguageSupport.DocumentationHandler<DOC> docHandler = Utils.cast(getDocHandler(lang, configuredLang));
        AC compile = compileHandler.getTask(project, sourceSet);
        // if there is a doc task, then it needs to have the source added
        DOC doc = (docHandler == null) ? null : docHandler.getTask(project, sourceSet);
        Modularity compileMod = LanguageSupport.getTaskModularity(compile);
        // if a source jar is registered for the source, then add to the source jar task
        Jar sourceJar = hasSourceJar ? (Jar) project.getTasks().getByName(sourceSet.getSourcesJarTaskName()) : null;
        // when using the jar, then "main" sources are not depended on. but otherwise they are
        Test test = isTest() ? (Test) project.getTasks().getByName(version.getTestName()) : null;
        boolean testUsesJar = version.getIsUseJarInTests();
        Map<String, LanguageSupport> languages = Utils.getExtension(project).languages;
        for (SourceSet dependSource : dependSources) {
            // if there is no depends on languages, then it depends on every language
            Set<String> dependLangNames = (configuredLang != null && configuredLang.getDependsOnLanguages().isPresent())
                    ? configuredLang.getDependsOnLanguages().get() : languages.keySet();
            dependLangNames = Utils.filterLanguages(project, dependSource, dependLangNames);
            for (String dependLangName : dependLangNames) {
                LanguageSupport dependLang = languages.get(dependLangName);
                AbstractCompile dependCompile = dependLang.getCompilerHandler().getTask(project, dependSource);
                compileHandler.addDependency(project, compile, dependCompile);

                // if this is a test source and the test does not use the jar or is a test source then it needs to be added
                if (test != null && (!testUsesJar || Utils.isTestSource(dependSource))) {
                    test.dependsOn(dependCompile);
                    Modularity dependsMod = LanguageSupport.getTaskModularity(dependCompile);
                    // if the test is modular, and the dependency is a module patch, then need to add to --patch-module
                    if (test.getModularity().getInferModulePath().get() && compileMod != null
                            && compileMod.state == Modularity.State.IS_MODULE && dependsMod != null
                            && dependsMod.state == Modularity.State.MODULE_PATCH) {
                        // add patch for the output directory of the dependent
                        PatchProvider provider = PatchProvider.getProvider(test.getJvmArgumentProviders());
                        provider.add(dependsMod.moduleName, dependCompile.getDestinationDir());
                    } else {
                        // otherwise, can just add to the classpath
                        test.setClasspath(test.getClasspath().plus(project.files(dependCompile.getDestinationDir())));
                    }
                }

            }
            if (doc != null) {
                docHandler.addSourceSet(project, doc, dependSource);
            }
            if (sourceJar != null) {
                sourceJar.from(dependSource.getAllSource());
            }
        }
    }

    @Override
    public void dependsOn(Object version) {
        dependsOn(version, null);
    }

    @Override
    public void dependsOn(Object version, Action<VersionDependency> configure) {
        Utils.notNull(version, "version");
        JavaVersion dependsOnVer = JavaVersion.toVersion(version);
        JavaVersion srcVersion = this.version.getVersion();

        if (this.version instanceof VersionBase) {
            throw new IllegalArgumentException("base version cannot depend on other versions");
        }
        // can only depend on prior versions, not future ones
        if (!srcVersion.isCompatibleWith(dependsOnVer)) {
            throw new IllegalArgumentException("version " + srcVersion + " cannot depend on " + dependsOnVer);
        }
        VersionDependencyImpl verDep = dependsOn.computeIfAbsent(dependsOnVer, (v)-> objFactory.newInstance(VersionDependencyImpl.class));
        if (configure != null) {
            configure.execute(verDep);
        }
    }

    @Override
    protected VersionSourceLanguageImpl doCreate(String name) {
        MRJarExtensionImpl mrjar = Utils.getExtension(getProject());
        LanguageSupport support = mrjar.languages.get(name);
        if (support == null) {
            throw new IllegalStateException("Language " + name + " is not a recognized JVM language");
        }
        VersionSourceLanguageImpl lang = objFactory.newInstance(VersionSourceLanguageImpl.class, this, support);
        return lang;
    }

    private LanguageSupport.DocumentationHandler<?> getDocHandler(LanguageSupport lang, VersionSourceLanguage configuredLang) {
        return (configuredLang != null && ((VersionSourceLanguageImpl) configuredLang).hasDoc) ?
                lang.getDocumentationHandler() : null;
    }

    @Override
    public Property<String> getPatchesModule() {
        return patchesModule;
    }

    Project getProject() {
        return version.project;
    }

    private boolean isTest() {
        return Utils.isTestSource(sourceSet);
    }

    void postConfigure() {
        Project project = getProject();
        // configure toolchains and modularity
        Modularity modularity = new DslObject(sourceSet).getExtensions().findByType(Modularity.class);
        // but update modularity for the source set if it's a module patch
        if (modularity != null && patchesModule.getOrElse(null) != null) {
            modularity.moduleName = patchesModule.get();
            modularity.state = Modularity.State.MODULE_PATCH;
        }
        Utils.setOptions(project, javaToolchains, sourceSet, modularity, version.getIsUseToolchain(), version.getVersion());

        if (version instanceof VersionBase) {
            // no later code applies to base versions
        }

        // configure any documentations in the version
        MRJarExtensionImpl ext = Utils.getExtension(project);
        Map<String, LanguageSupport> langs = ext.languages;
        this.stream().map((lang)-> getDocHandler(langs.get(lang.getName()), lang))
            .filter(Objects::nonNull).forEach(handler ->
                Utils.setOptions(project, javaToolchains, sourceSet, Utils.cast(handler),
                        version.getIsUseToolchain(), modularity, version.getVersion())
        );

        // this must be done first to ensure that the most appropriate classpath is chosen when there are duplicates
        if (isTest() && version.main != null) {
            // depend on any corresponding "main" source when a "test" source
            addDependentSources(Collections.singletonList(version.main.sourceSet));
        }

        // execute in reverse order to match how multi-release jar behaves using highest versions first
        List<JavaVersion> dependVersions = new ArrayList<>(dependsOn.keySet());
        Collections.reverse(dependVersions);
        for (JavaVersion version : dependVersions) {
            postConfigure(version, dependsOn.get(version));
        }
    }

    private void postConfigure(JavaVersion dependsOnVer, VersionDependencyImpl verDep) {

        Project project = getProject();
        MRJarExtensionImpl mrJar = Utils.getExtension(project);
        SourceSetContainer sourceSets = Utils.getSourceSets(project);
        ConfigurationContainer configurations = project.getConfigurations();

        // get the source set that identifies the java version
        List<SourceSet> dependSources = new ArrayList<>();
        boolean thisIsTest = isTest();
        if (dependsOnVer == Utils.getExtension(project).getBaseVersion()) {
           if (verDep.getDependsOnSource().getOrElse(Boolean.TRUE)) {
               dependSources.add(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
           }
           if (thisIsTest && verDep.getDependsOnTest().getOrElse(Boolean.TRUE)) {
               dependSources.add(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME));
           }
        } else {
            VersionAdd targetVer = mrJar.getVersions().get(dependsOnVer);
            if (verDep.getDependsOnSource().getOrElse(Boolean.TRUE)) {
                if (targetVer.main == null) {
                    throw new IllegalStateException("corresponding SourceSet for " + dependsOnVer + " main sources is not defined");
                }
                dependSources.add(targetVer.main.sourceSet);
            }
            if (verDep.getDependsOnTest().getOrElse(Boolean.FALSE)) {
                if (targetVer.test == null) {
                    throw new IllegalStateException("corresponding SourceSet for " + dependsOnVer + " test sources is not defined");
                }
                dependSources.add(targetVer.test.sourceSet);
            }
        }

        // extend configurations if applicable
        if (verDep.getExtendConfigurations().getOrElse(Boolean.FALSE)) {
            extendConfiguration(configurations, mrJar.languages.values(), sourceSet, dependSources);
        }

        addDependentSources(dependSources);
    }

    @Override
    public void registerSourceJar() {
        // register the source jar
        hasSourceJar = true;
        Project project = getProject();
        configureDocumentationVariantWithArtifact(sourceSet.getSourcesElementsConfigurationName(), sourceSet.getName(),
                SOURCES, Collections.emptyList(), sourceSet.getSourcesJarTaskName(), sourceSet.getAllSource(),
                findJavaComponent(project.getComponents()), project.getConfigurations(), project.getTasks(),
                objFactory);
        Configuration config = project.getConfigurations().getByName(sourceSet.getSourcesElementsConfigurationName());
        config.getAttributes().attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                Integer.parseInt(version.getVersion().getMajorVersion()));
        Jar sourceJar = (Jar) project.getTasks().getByName(sourceSet.getSourcesJarTaskName());
        /* and flag that duplicates should be excluded (ignored).
         * This will cause higher versions to be included and lower versions be excluded */
        sourceJar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
    }
}

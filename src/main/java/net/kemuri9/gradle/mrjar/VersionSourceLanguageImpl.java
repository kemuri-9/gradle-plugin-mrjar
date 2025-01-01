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

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.compile.AbstractCompile;

import net.kemuri9.gradle.mrjar.languages.LanguageSupport;
import net.kemuri9.gradle.mrjar.languages.LanguageSupport.DocumentationHandler;

class VersionSourceLanguageImpl implements VersionSourceLanguage {

    private final VersionSourceImpl source;
    final LanguageSupport language;
    private final SetProperty<String> dependsOnLanguages;
    boolean hasDoc;

    @javax.inject.Inject
    public VersionSourceLanguageImpl(VersionSourceImpl source, LanguageSupport language, ObjectFactory objFactory) {
        this.source = source;
        this.language = language;
        dependsOnLanguages = objFactory.setProperty(String.class);
        // specifically set to null, otherwise isPresent returns true
        dependsOnLanguages.value((Iterable<String>) null);
    }

    @Override
    public void compile(Action<? super AbstractCompile> configure) {
        if (language.getCompilerHandler() == null) {
            throw new UnsupportedOperationException("language " + language.getName() + " compilation is not supported");
        }
        language.getCompilerHandler().configure(source.getProject(), source.sourceSet, configure);
    }

    @Override
    public void documentation(Action<? super SourceTask> configure) {
        DocumentationHandler<?> handler = language.getDocumentationHandler();
        if (handler == null) {
            throw new UnsupportedOperationException("language " + language.getName() + " documentation is not supported");
        }
        if (!hasDoc) {
            hasDoc = true;
            handler.register(source.getProject(), source.sourceSet, source.version.getVersion());
        }
        handler.configure(source.getProject(), source.sourceSet, configure);
    }

    @Override
    public SetProperty<String> getDependsOnLanguages() {
        return dependsOnLanguages;
    }

    @Override
    public String getName() {
        return language.getName();
    }

    @Override
    public void src(Action<SourceDirectorySet> configure) {
        language.source(source.getProject(), source.sourceSet, configure);
    }
}

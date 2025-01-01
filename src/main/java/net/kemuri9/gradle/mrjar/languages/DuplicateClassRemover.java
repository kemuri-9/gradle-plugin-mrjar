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
package net.kemuri9.gradle.mrjar.languages;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.gradle.api.specs.Spec;

/**
 * When performing version overrides, there are often multiple versions of the same source code files targeting
 * different major releases. In these situations, the versions for the non-latest release
 * need to be excluded. This assists in performing such exclusions.
 */
public class DuplicateClassRemover implements Action<FileVisitDetails>, Spec<FileTreeElement> {

    /** {@link Set} of {@link RelativePath}s that have been encountered */
    protected final Set<RelativePath> paths;

    /** {@link Set} of {@link File}s that represent a duplicated {@link RelativePath} that should be excluded */
    protected final Set<File> duplicates;

    /**
     * Create a new {@link DuplicateClassRemover}
     */
    public DuplicateClassRemover() {
        paths = new HashSet<>();
        duplicates = new HashSet<>();
    }

    @Override
    public void execute(FileVisitDetails t) {
        if (t.isDirectory()) {
            return;
        }
        if (!paths.add(t.getRelativePath())) {
            duplicates.add(t.getFile());
        }
    }

    /**
     * Retrieve the duplicate class files that need to be excluded
     * @return duplicate class files to exclude
     */
    public Set<File> getDuplicates() {
        return duplicates;
    }

    @Override
    public boolean isSatisfiedBy(FileTreeElement element) {
        return duplicates.contains(element.getFile());
    }
}

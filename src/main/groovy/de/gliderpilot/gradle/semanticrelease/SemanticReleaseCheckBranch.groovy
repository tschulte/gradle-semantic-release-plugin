/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gliderpilot.gradle.semanticrelease

import groovy.transform.PackageScope

/**
 * Created by tobias on 7/21/15.
 */
class SemanticReleaseCheckBranch {

    Set<String> includes = [] as Set
    Set<String> excludes = [] as Set

    SemanticReleaseCheckBranch() {
        include(/^master$/, /^(?:release[-\/])?\d+(?:\.\d+)?\.x$/)
    }

    void include(String... patterns) {
        includes.addAll(patterns)
    }

    void exclude(String... patterns) {
        excludes.addAll(patterns)
    }

    @PackageScope
    boolean isReleaseBranch(String branchName) {
        if (includes.any { branchName ==~ it })
            return true

        if (excludes.any { branchName ==~ it })
            return false

        return includes.isEmpty()
    }

}

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
package de.gliderpilot.gradle.semanticrelease.integration

import de.gliderpilot.gradle.semanticrelease.SemanticReleasePlugin
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginIntegrationSpec
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.Specification

class SemanticReleaseIntegrationAuxScriptSpec extends SemanticReleasePluginIntegrationSpec {

    @Override
    def setupGradleProject(){
        buildFile << '''
            apply from:'release.gradle'
            println version
        '''
        file('release.gradle') << """
            buildscript{

                repositories{
                    jcenter()
                }

                dependencies{
                    classpath files('${getPluginCompileDir()}')
                    classpath files('${getPluginCompileDir().replace('/classes/','/resources/')}')
                    classpath "org.ajoberstar:gradle-git:1.3.0"
                    classpath 'com.jcabi:jcabi-github:0.23'
                }

            }

            apply plugin: de.gliderpilot.gradle.semanticrelease.SemanticReleasePlugin

"""
}

    def getPluginCompileDir(){
        URL url=SemanticReleasePlugin.class.getResource(SemanticReleasePlugin.class.getSimpleName() + ".class")
        String classFilePath=new File(url.toURI()).absolutePath
        if(isWindows())
            classFilePath = classFilePath.replace('\\','/')
        String classFileRelative = SemanticReleasePlugin.class.getName().replace('.', '/') + ".class"
        classFilePath-classFileRelative
    }
}

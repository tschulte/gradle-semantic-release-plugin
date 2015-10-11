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

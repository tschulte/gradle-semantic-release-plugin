Gradle Plugin Project template
------------------------------

You've just created a basic Gradle plugin project

The project's structure is laid out as follows

    <proj>
      |
      +- src
          |
          +- main
              |
              +- groovy
              |
                 // plugin sources
              |
              +- resources
              |
                 // plugin resources
          +- test
              |
              +- groovy
              |
                 // plugin tests

Execute the following command to compile and package the project

    ./gradlew build

Execute the following command to deploy to Artifactory

    ./gradlew artifactoryPublish

Execute the following command to deploy to Bintray

    ./gradlew bintrayUpload


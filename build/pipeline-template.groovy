#!groovy

pipeline {
    agent {
        label 'maven'
    }

    options {
        skipStagesAfterUnstable()
    }

    stages {

        /*
         * Pull the source code from Git.
         * Note: If needed, credentials should be created in Jenkins and passed to git using the 'credentials' parameter.
         */
        stage('Pull from Git') {
            steps {
                /* 
                 *  TODO: Pull the source code from git here.
                 *  There a plug in you can use for that. The syntax is:
                 *  git branch: <your git branch>, url: <git repository url>
                 */
                
                echo "Pull from git"
                // Your code here
            }
        }

        /* 
         * Get the version information from the POM-file.
         * These version numbers are later used to tag outputs such as binaries and images.
         */
        stage('Get version information') {
            steps {
                /*
                 *  TODO: Read the version info from the POM file.
                 *  There's also a plugin to read from the POM file. The syntax:
                 *  def pom = readMavenPom file: 'pom.xml'
                 *
                 *  It can also be a good idea to append the current build number, which you can get from currentBuild.number.
                 *  
                 *  Note that the POM file is in the demo folder, so you can wrap your code in 
                 *  a 'dir' block:
                 *  dir('demo') {
                 *      // You're now in the demo folder
                 *  }
                 *  
                 */

                echo "Get version information"
                // Your code here                
            }
        }

        /* 
         * Execution Maven build, but do no run unit tests yet - they will be executed in the next step.
         */
        stage('BUILD - execute Maven build') {
            steps {
                /*
                *  TODO: Execute the Maven build. 
                *  Remember to use the correct directory and the sh command
                */                
                
                echo "BUILD - execute Maven build"
                // Your code here
            }
        }

        /*
         * Use Maven to run unit tests.
         */ 
        stage('BUILD - run unit tests') {
            steps {
                /*
                *  TODO: Execute unit tests using Maven
                */

                echo "BUILD - run unit tests"
                // Your code here                
            }
        }

        /*
         *  Create a new image from the source code in the build namespace.
         *  This will create a new build config, if one does not already exist. 
         */
        stage('BUILD - Create image in build namespace') {
            steps {
                /*
                 * TODO: Do the following:
                         1. Create a new image stream, if it doesn't exist
                            Use the template called imagestream-template.yaml
                         2. Create a new build config, if it doesn't exist
                            Use the template called binary-s2i-template.yaml
                            A good base image is java:8 in the openshift namespace.
                         3. Start the build, by uploading the content of your ./target folder
                            There's a startBuild function of the buildconfig class. The --wait switch will wait for the build to complete
                         4. When the build is done, tag the newly created image with the version from the POM file.
                 */

                echo "BUILD - Create image in build namespace"
                // Your code here
            }
        }

        stage('DEV - Deploy to development environment') {
            steps {
                /*
                 *  TODO: Do the following:
                 *  1. Create the deployment config and service, if it doesn't exist. 
                 *     Use the template called dc-and-service.yaml. 
                 *     The application listens on port 8080, so the service port should be 80, and the target 8080.
                 *  2. Tag the container image with "dev".
                 */
                
                echo "DEV - Deploy to development environment"
                // Your code here
            }
        }

    }

}
#!groovy

APP_NAME="demo"
GIT_URL="https://github.com/leskil/openshift-deployment-handson"
GIT_BRANCH="master"
BASE_IMAGE="java"
BASE_IMAGE_TAG="8"
BASE_IMAGE_NAMESPACE="openshift"

BUILD_NAMESPACE="le-handson-build"
DEV_NAMESPACE="le-handson-dev"
IMAGESTREAM_NAME="demo"
BUILD_CONFIG_NAME="demo"

DEV_TAG=""
PROD_TAG=""

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
                git branch: GIT_BRANCH, url: GIT_URL
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

                dir('demo') {
                    script {
                        def pom = readMavenPom file: 'pom.xml'
                        DEV_TAG  = "${pom.version}-${currentBuild.number}"
                        PROD_TAG = pom.version
                        echo "Dev tag: ${DEV_TAG}. Prod tag: ${PROD_TAG}"
                    }                    
                }                
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
                dir('demo') {
                    echo "Executing Maven build"
                    sh "mvn clean package -DskipTests"
                }
            }
        }

        /*
         * Use Maven to run unit tests.
         */ 
        stage('BUILD - run unit tests') {
            /*
             *  TODO: Execute unit tests using Maven
             */
            steps {
                dir ('demo') {
                    echo "Running tests"
                    sh "mvn test"
                }
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
                script {
                    openshift.withCluster() {
                        openshift.withProject(BUILD_NAMESPACE) {

                            // Process the imagestream template:
                            def isTemplate = openshift.process(readFile(file:'build/imagestream-template.yaml'), 
                                    '-p', "APP_NAME=${APP_NAME}", 
                                    '-p', "IMAGESTREAM_NAME=${IMAGESTREAM_NAME}", 
                                    '-p', "REVISION=development")
                            // Apply any changes to OpenShift:
                            openshift.apply(isTemplate)

                            // Process the buildconfig template:
                            def bcTemplate = openshift.process(readFile(file:'build/binary-s2i-template.yaml'),
                                                    '-p', "APP_NAME=${APP_NAME}",
                                                    '-p', "NAME=${BUILD_CONFIG_NAME}",
                                                    '-p', "BASE_IMAGESTREAM_NAMESPACE=${BASE_IMAGE_NAMESPACE}",
                                                    '-p', "BASE_IMAGESTREAM=${BASE_IMAGE}",
                                                    '-p', "BASE_IMAGE_TAG=${BASE_IMAGE_TAG}",
                                                    '-p', "TARGET_IMAGESTREAM=${IMAGESTREAM_NAME}",
                                                    '-p', "REVISION=development")
                            // Apply any changes to OpenShift:
                            openshift.apply(bcTemplate)                            

                            // Get the newly created buildconfig, and start a build:
                            def bc = openshift.selector("bc/${BUILD_CONFIG_NAME}")
                            bc.startBuild('--from-dir=demo/target/', '--wait=true')

                            // Tag the created (:latest) image with the version from the POM file:
                            openshift.tag("${BUILD_NAMESPACE}/${IMAGESTREAM_NAME}:latest", "${BUILD_NAMESPACE}/${IMAGESTREAM_NAME}:${DEV_TAG}")                            
                        }
                    }
                }
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
                script {
                    openshift.withCluster() {
                        openshift.withProject(DEV_NAMESPACE) {
                            
                            // Tag the image we created with the dev tag, which is the tag the deployment config is using:
                            openshift.tag("${BUILD_NAMESPACE}/${IMAGESTREAM_NAME}:${DEV_TAG}", "${BUILD_NAMESPACE}/${IMAGESTREAM_NAME}:dev")

                            // Process the deployment config and service template:
                            def dc = openshift.process(readFile(file:'build/dc-and-service.yaml'),
                                                '-p', "APPNAME=${APP_NAME}",
                                                '-p', "IMAGESTREAMNAMESPACE=${BUILD_NAMESPACE}",
                                                '-p', "IMAGESTREAM=${IMAGESTREAM_NAME}",
                                                '-p', "IMAGESTREAMTAG=dev",
                                                '-p', "SERVICEPORT=80",
                                                '-p', "SERVICETARGETPORT=8080")

                            // Apply the changes:
                            openshift.apply(dc)
                        }
                    }
                }
            }
        }

    }

}
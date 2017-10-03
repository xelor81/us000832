mvnHome = tool 'M3'

mvnRepo = "${env.JOB_MAVEN_DIR}"

mvnRepoArg = "-Dmaven.repo.local=\"${mvnRepo}\""

mvn = "${mvnHome}/bin/mvn ${mvnRepoArg}"


def buildFailed() {
    if (currentBuild.result == null ||
    currentBuild.result == '' ||
    currentBuild.result == 'SUCCESS') {
        return false
    }
    return true
}

//------------------------------------------------------------------------------

def buildStatus() {
    return currentBuild.result
}

//------------------------------------------------------------------------------

def checkBuildStatus(oldStatus) {
    if (oldStatus != currentBuild.result &&
    buildFailed()) {
        error "Fail the build"
    }
}

def checkout(branch, gitTarget, condition) {
    stage('Checkout') {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //**************************************************************
                checkout([$class                           : 'GitSCM',
                    branches                         : [[name: branch]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions                       : [],
                    submoduleCfg                     : [],
                    userRemoteConfigs                : [[credentialsId: 'svcacct_paasjenkgit',
                            url          : gitTarget]]])

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}


def pushStack(environment, awsRegion, awsProfile, condition) {
    stage("Push ${environment} Stack to ${awsRegion}") {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //**************************************************************
                sh "aws configure set preview.cloudfront true"
                sh """\
                    export ANSIBLE_FORCE_COLOR=true
                    pwd && cd devops && pwd && ls && whoami && bash push.sh ${environment} ${awsRegion} ${awsProfile}
                """

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}


def coverage(condition) {
    stage("Jacoco Coverage}") {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //**************************************************************
                //----------------------------------------------------------
                // Archive the Test coverage results (Jacoco plugin)
                //----------------------------------------------------------
                // For config see the constructor at:
                // https://github.com/jenkinsci/jacoco-plugin/blob/master/
                //           src/main/java/hudson/plugins/jacoco/
                //           JacocoPublisher.java
                //----------------------------------------------------------
                step([
                        $class          : "JacocoPublisher",
                        execPattern     : "**/jacoco.exec",
                        inclusionPattern: "**/com/mcafee/**/*",
                        exclusionPattern: "**/*CloudModelGeneratorTool*,**/mcafee/cloudsupport/testutils/**/*," +
                                "**/mcafee/cloudsupport/tools/*,**/mcafee/cloudsupport/shell/Main*"
                ])
                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}

def runIntegrationTest(environment, awsRegion, awsProfile, archiveTestResults , condition) {
    stage("Integration tests: ${environment}") {
        if (condition.toBoolean()) {
            dir('test/integration-test') {
                def reportNameSuffix = "IntegrationTest-${environment}";

                catchError {
                    def oldStatus = buildStatus()
                    //**********************************************************

                    sh """\
                        ${mvn} integration-test -Dsurefire.reportNameSuffix=${
                        reportNameSuffix
                    } -Pintegration-test-module -DargLine='-Denvironment=${environment} -DawsRegion=${
                        awsRegion
                    } -Daws.profile=${awsProfile}'
                    """

                    //**********************************************************
                    checkBuildStatus(oldStatus)
                }
                //--------------------------------------------------------------
                catchError {
                    def oldStatus = buildStatus()
                    //**********************************************************

                    if (archiveTestResults) {
                        step([
                            $class: "JUnitResultArchiver",
                            testResults: "target/surefire-reports/TEST-*-${reportNameSuffix}.xml"
                        ])
                    }

                    //**********************************************************
                    checkBuildStatus(oldStatus)
                }
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}


def runFunctionalTest(environment, awsRegion, awsProfile, archiveTestResults , condition) {
    stage("Functional tests: ${environment}") {
        if (condition.toBoolean()) {
            dir('test/func-test') {
                def reportNameSuffix = "FunctionalTest-${environment}";

                catchError {
                    def oldStatus = buildStatus()
                    //**********************************************************

                    sh """\
                        ${mvn} integration-test -Dsurefire.reportNameSuffix=${
                        reportNameSuffix
                    } -Pfunc-test -DargLine='-Denvironment=${environment} -DawsRegion=${
                        awsRegion
                    } -Daws.profile=${awsProfile}'
                    """

                    //**********************************************************
                    checkBuildStatus(oldStatus)
                }
                //--------------------------------------------------------------
                catchError {
                    def oldStatus = buildStatus()
                    //**********************************************************

                    if (archiveTestResults) {
                        step([
                                $class: "JUnitResultArchiver",
                                testResults: "target/surefire-reports/TEST-*-${reportNameSuffix}.xml"
                        ])
                    }

                    //**********************************************************
                    checkBuildStatus(oldStatus)
                }
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}

//==============================================================================
//  build
//  --------------------
//  Clean builds the project and archives the test results if requested
//==============================================================================
def build(archiveTestResults, condition) {
    stage("Build project") {
        if (condition) {
            def reportNameSuffix = "UnitTests";

            catchError {
                def oldStatus = buildStatus()
                //**************************************************************

                sh """\
                    ${mvn} clean install -Dsurefire.reportNameSuffix=${reportNameSuffix}
                """

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
            //------------------------------------------------------------------
            catchError {
                def oldStatus = buildStatus()
                //**************************************************************

                if (archiveTestResults) {
                    //----------------------------------------------------------
                    // Archive the unit test results
                    //----------------------------------------------------------
                    step([
                        $class     : "JUnitResultArchiver",
                        testResults: "**/target/surefire-reports/TEST-*-${reportNameSuffix}.xml"
                    ])

                    //----------------------------------------------------------
                    // Archive the static analysis results
                    //----------------------------------------------------------
                    step([
                        $class         : "FindBugsPublisher",
                        pattern        : "**/findbugsXml.xml",
                        includePattern : "",
                        excludePattern : "",
                        canComputeNew  : false,
                        canRunOnFailed : true,
                        isRankActivated: true,
                        defaultEncoding: "",
                        healthy        : "",
                        unHealthy      : ""
                    ])
                }

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else {
            println "  ===== SKIPPED ====="
        }
    }
}


//==============================================================================
//  coverityScan
//  --------------------
//  Runs the coverity scan
//==============================================================================
def coverityScan(condition) {
    //--------------------------------------------------------------------------
    stage("Coverity scan") {
        //--------------------------------------------------------------------------
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //******************************************************************

                println "Scanning workspace: ${env.WORKSPACE}"

                dir('tools/coverity') { sh """\
                    bash ./coverity.sh ${env.WORKSPACE}
                """ }

                //******************************************************************
                checkBuildStatus(oldStatus)
            }
        } else {
        }
    }
}


//==============================================================================
//  exportCoverityLogs
//  --------------------
//  Exports the coverity logs
//==============================================================================
def exportCoverityLogs(condition) {
    stage("Export Coverity Logs") {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //**************************************************************

                dir('coverity.log/') { sh """\
                        tar -cvzf output.tar.gz output/
                    """ }

                step([
                    $class     : 'ArtifactArchiver',
                    artifacts  : "**/coverity.log/output.tar.gz",
                    fingerprint: true
                ])

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else { println "  ===== SKIPPED =====" }
    }
}


//==============================================================================
//  buildAngular
//  --------------------
//  Run npm to build AngularJS project
//==============================================================================
def buildAngular() {
    stage('Build AngularJS') { sh """\
            bash ./npmbuild.sh ${env.CloudSupEnvironment} ${env.awsRegion} ${env.awsProfile}
        """ }
}


//==============================================================================
//  angularLint
//  --------------------
//  Run npm lint to check AngularJS project
//==============================================================================
def angularLint() {
    stage('Lint check for AngularJS code') { sh """\
            npm run lint > /tmp/lint.log && grep src /tmp/lint.log |wc -l
        """ }
}


//==============================================================================
//  uploadS3
//  --------------------
//  Run aws s3 cp command to upload file to S£
//==============================================================================
def uploadS3(source,CloudSupEnvironment) {
    stage('UploadDashboardS3') { sh """\
            aws s3 cp ${source}/ s3://cloudsupport-${CloudSupEnvironment}.cloudplatform.mcafee.com/home --recursive --profile=${awsProfile}
        """ }
}



//==============================================================================
//  protractor
//  --------------------
//  Execute protractor tests
//==============================================================================
def protractor() {
    stage('Protractort check for AngularJS code') {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) { sh '''#!/bin/bash -xe
                set -x
                killall Xvfb || true
                kill $(ps -ef | grep [w]ebdriver | awk '{ print $2 }') || true
                export DISPLAY=:99
                Xvfb :99 -ac -screen 0 1980x1020x24 &
                webdriver-manager update
                nohup webdriver-manager start &>/dev/null &
                sleep 10
                protractor protractor.conf.js
                webdriver-manager shutdown 
                kill $(ps -ef | grep [w]ebdriver | awk '{ print $2 }') || true
                #tmux kill-server
                killall Xvfb
            ''' }
    }
}

//==============================================================================
//  Take Snapshot of Elasticsearch
//  --------------------
//  Run ansible play to backup Elasticsearch
//==============================================================================
def takeEsSnapshot(BuildEnvironment,AwsRegion,AwsProfile) {
    stage("Take Snapshot of ElasticSearch ${BuildEnvironment} Enviro in ${AwsRegion}") { sh """\
            pwd && cd devops/solutionRTO && pwd && ls && whoami && bash elasticSearchBackup.sh ${BuildEnvironment} ${AwsRegion} ${AwsProfile}
        """ }
}

//==============================================================================
//  Restore ElasticSearch From Snapshot
//  --------------------
//  Run ansible play to restore Elasticsearch
//==============================================================================
def restoreEsFromSnapshot(BuildEnvironment,AwsRegion,AwsProfile,SourceBucket,SourceSnapshot) {
    stage("Restore Snapshot to ${BuildEnvironment} ElasticSearch Enviro in ${AwsRegion}") { sh """\
            pwd && cd devops/solutionRTO && pwd && ls && whoami && bash elasticSearchRestore.sh ${BuildEnvironment} ${AwsRegion} ${AwsProfile} ${SourceBucket} ${SourceSnapshot}
        """ }
}

//==============================================================================
//  Call EsSnapshot Pipeline from Another Pipeline
//  --------------------
//  Run ansible play to backup Elasticsearch
//==============================================================================
def launchTakeEsSnapshot(BuildEnvironment, AwsRegion, AwsProfile) {
    stage("Starting ElasticSearch ${BuildEnvironment} Environment Snapshots Job") {
        build job: "ElasticSearch ${BuildEnvironment} Environment Snapshots",
        parameters: [[
                $class: 'StringParameterValue',
                name: 'BuildEnvironment',
                value: "${BuildEnvironment}"
            ], [
                $class: 'StringParameterValue',
                name: 'AwsRegion',
                value: "${AwsRegion}"
            ], [
                $class: 'StringParameterValue',
                name: 'AwsProfile',
                value: "${AwsProfile}"
            ]]
    }
}

//==============================================================================
//  Call Restore ElasticSearch From Snapshot From another Pipeline
//  --------------------
//  Run ansible play to restore Elasticsearch
//==============================================================================
def launchRestoreEsFromSnapshot(BuildEnvironment,AwsRegion,AwsProfile,SourceBucket,SourceSnapshot) {
    stage("Starting Restore Snapshot to ${BuildEnvironment} ElasticSearch Environment Job") {
        build job: "ElasticSearch ${BuildEnvironment} Environment Restore",
        parameters: [
            [
                $class: 'StringParameterValue',
                name: 'BuildEnvironment',
                value: "${BuildEnvironment}"
            ],
            [
                $class: 'StringParameterValue',
                name: 'AwsRegion',
                value: "${AwsRegion}"
            ],
            [
                $class: 'StringParameterValue',
                name: 'AwsProfile',
                value: "${AwsProfile}"
            ],
            [
                $class: 'StringParameterValue',
                name: 'SourceBucket',
                value: "${SourceBucket}"
            ],
            [
                $class: 'StringParameterValue',
                name: 'SourceSnapshot',
                value: "${SourceSnapshot}"
            ]
        ]
    }
}

def getApproval(solution, environment, awsRegion, awsProfile, action, condition) {
    stage("Approve ${action}: ${environment}") {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()

                input "Are you sure you want to ${action} (${solution} in ${environment}/${awsRegion}/${awsProfile})?"

                def emailBody = """\
                    A(n) ${action} has been triggered for ${solution} in ${environment}/${awsRegion}/${awsProfile}!

                    ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}

                    Check console output at ${env.BUILD_URL} to view the results.
                """

                def emailSubject = "Databus Notification: A(n) ${action} has been triggered for ${solution}/ in ${environment}/${awsRegion}/${awsProfile}"

                emailext body: emailBody,
                subject: emailSubject,
//              replyTo: '$DEFAULT_REPLYTO',
                to: '$DEFAULT_RECIPIENTS',
                sendToIndividuals: true

                checkBuildStatus(oldStatus)
            }
        }
        else { println "  ===== SKIPPED =====" }
    }
}

//==============================================================================
//  tagDeployment
//  --------------------
//  Tags the deployment.
//==============================================================================
def tagDeployment(solution, environment, awsRegion, awsProfile, condition) {
    stage("Tag deployment: ${environment}") {
        if (condition.toBoolean()) {
            catchError {
                def oldStatus = buildStatus()
                //******************************************************************

                def tagSuffix = "${env.JOB_NAME}-${env.BUILD_NUMBER}"
                def tag = "${solution}_${environment}_${awsRegion}_${awsProfile}-${tagSuffix}"
                def tagMessage = "${solution} deployed to ${solution}_${environment}_${awsRegion}_${awsProfile}. Job ${env.JOB_NAME}@${env.BUILD_NUMBER}"
                sh """\
                    git tag -a $tag -m "$tagMessage"
                    git push --tags
                """

                def buildDescription = """\
                    $tag
                    $tagMessage
                """

                currentBuild.description = "Tag: $buildDescription"

                //**************************************************************
                checkBuildStatus(oldStatus)
            }
        } else { println "  ===== SKIPPED =====" }
    }
}

//==============================================================================
//  telegrafBuild
//  --------------------
//  Run the script to build the telegraf executable with the plugins in the codebase
//==============================================================================
def telegrafBuild() {
    stage("Telegraf Build") { 
            dir('docker/TELEGRAF_BUILD'){
                sh """\
                    sudo bash ./call_build.sh $env.BUILD_TAG
                """
            }
        }
}

//==============================================================================
//  telegrafTests
//  --------------------
//  Run the script to test telegraf
//==============================================================================
def telegrafTests() {
    stage("Telegraf Tests") { 
            dir('docker/TELEGRAF_LAUNCH'){
                sh """\
                    sudo python ./runTests.py $env.BUILD_TAG
                """
            }
        }
}

//==============================================================================
//  telegrafImageBuild
//  --------------------
//  Run the script to build new Docker telegraf Image
//==============================================================================
def telegrafImageBuild() {
    stage("Telegraf Docker Image Build") { 
            dir('docker/TELEGRAF_LAUNCH/TELEGRAF_RUN_DOCKER/'){
                sh """\
                    bash newTelegrafDockerBuild.sh ${env.Environment} ${env.Version} $env.BUILD_TAG
                """
            }
        }
}

//==============================================================================
//  telegrafBuildCleanup
//  --------------------
//  Run the script to clean the telegraf build
//==============================================================================
def telegrafBuildCleanup() {
    stage("Telegraf Build Cleanup") { 
            dir('docker/TELEGRAF_BUILD'){
                sh """\
                    sudo ./clean_build.sh $env.BUILD_TAG
                """
            }
        }
}

//==============================================================================
//  telegrafEcsPushImage
//  --------------------
//  Run the script to push telegraf image to AWS Ecs registry
//==============================================================================
def telegrafEcsPush() {
    stage("Telegraf Ecs Push") { 
            dir('docker/TELEGRAF_LAUNCH/TELEGRAF_RUN_DOCKER'){
                sh """\
                    bash newTelegrafDockerEcsPush.sh ${env.Environment} ${env.Version}
                """
            }
        }
}

//==============================================================================
//  coberturaPublish
//  --------------------
//  Run the script to push telegraf image to AWS Ecs registry
//==============================================================================
def coberturaPublish() {
    stage('Cobertura Publish') {
        step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: "docker/TELEGRAF_BUILD/BUILD_jenkins-TelegrafBuild-Docker-Dev-${env.BUILD_NUMBER}/shared_folder/unitTestCoverage.xml", failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
    }
}

//==============================================================================
//  finished
//  --------------------
//  Sends email notifications and fails the build if it has to
//==============================================================================
def finished() {
    //--------------------------------------------------------------------------
    stage "Finished"
    //    //--------------------------------------------------------------------------
    //    step([
    //            $class                  : "Mailer",
    //            notifyEveryUnstableBuild: true,
    //            sendToIndividuals       : true,
    //            recipients              : emailextrecipients([
    //                    [$class: "CulpritsRecipientProvider"],
    //                    [$class: "RequesterRecipientProvider"]
    //            ])
    //    ])
    //
    //    if (buildFailed()) {
    //        error "Mark build as failed"
    //    }
}

return this;

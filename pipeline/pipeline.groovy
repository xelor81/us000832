/**
 * Used for deploying environments
 * Pushes the stack first to a pre-environment, validates everything,
 * then pushes the stack to the live system and runs a minimal set of tests.
 *
 * @param CloudSupEnvironment
 *
 * @param awsRegion The aws region to which the stack is pushed
 * @param awsProfile The aws profile/account to which the stack is pushed
 *
 * @param withCoverity The public key used for the secret key encryption
 * @param needsApproval The deployment has to be approved. If so it will be also tagged
 * @param gitTarget The branch to build
 */
node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	env.JOB_MAVEN_DIR = "${env.WORKSPACE_ROOT_DIR}/${env.JOB_BASE_NAME}@m2";
	env.JAVA_HOME = "${tool 'jdk-8'}";

	env.STAGES_DIR = "${env.WORKSPACE_ROOT_DIR}/${env.JOB_BASE_NAME}/stages/scripts";
	stage('Checkout Stages') {
		//**************************************************************
		dir ("stages") {
			git branch: 'master', credentialsId: 'svcacct_paasjenkgit', url: 'git@github-lvs.corpzone.internalzone.com:operations/cloud_supportability-pipelines.git'
		}
		//**************************************************************
	} 
	def stages = load "${env.STAGES_DIR}/stages.groovy";

	def solution = "Cloud-Supportability"
	stages.checkout(gitTarget, 'git@github-lvs.corpzone.internalzone.com:operations/cloud_supportability-cloudsupport-core.git')

	if (withCoverity.toBoolean()) {
		stages.coverityScan()
		stages.exportCoverityLogs()
	}
	stages.buildMaven(true)

	stages.runIntegrationTest(CloudSupEnvironment, awsRegion, awsProfile, true)

	stages.jacocoCoverage()

	if (needsApproval.toBoolean()) {
		stages.getApproval(solution, CloudSupEnvironment, awsRegion, awsProfile, "deploy")
		stages.tagDeployment(solution, CloudSupEnvironment, awsRegion, awsProfile);
	}

	stages.pushStack(CloudSupEnvironment, awsRegion, awsProfile)

	// Functional test runs after the deployment, and does a basic test
	stages.runFunctionalTest(apiKeyId, apiKey, CloudSupEnvironment, awsRegion, awsProfile, true)

	stages.finished()
}

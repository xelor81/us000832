node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	
	stage('Checkout Github') {
		//**************************************************************
		git credentialsId: '513094ca-73b7-4145-8829-4474bbbb1c24', url: 'git@github.com:xelor81/us000832.git'
		//**************************************************************
	}
	dir("pipeline")
	def stages = load "./common_functions.groovy";
	
	stages.deploy_VM_NET()
}

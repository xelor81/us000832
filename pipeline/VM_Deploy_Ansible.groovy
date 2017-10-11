node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	def stages = load "./pipeline/common_functions.groovy";
	
	git_credentials='513094ca-73b7-4145-8829-4474bbbb1c24'
	git_url='git@github.com:xelor81/us000832.git'
	git_branch='master'
	
	//------------------ EXECUTE PIPELINES JOBS ---------------------------
	
	stages.git_checkout(env.git_branch, env.git_login, env.git_url)
	stages.deploy_VM_NET()
	
}

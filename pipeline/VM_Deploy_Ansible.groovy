node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	def stages = load "./pipeline/common_functions.groovy";
	
	git_login='513094ca-73b7-4145-8829-4474bbbb1c24'
	git_url='git@github.com:xelor81/us000832.git'
	git_branch='master'
	
	//------------------ EXECUTE PIPELINES JOBS ---------------------------
	println("ZZZZZZZZ: " + git_url)
	println("login: " + git_login)
	
	stages.git_checkout(git_branch, git_login, git_url)
	
	stages.deploy_VM_NET()
	
}

node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	
	def stages = load "./pipeline/common_functions.groovy";
	
	git_login='513094ca-73b7-4145-8829-4474bbbb1c24'
	git_url='git@github.com:xelor81/us000832.git'
	git_branch='master'
	
	//------------------ EXECUTE PIPELINES JOBS ---------------------------
	println("git URL:    " + git_url)
	println("git LOGIN:  " + git_login)
	println("git BRANCH: " + git_branch)
	
	//stages.ZPRINT()
	//stages.gitCHECK(git_branch, git_login, git_url)
	
	stages.setting_ansible_color()
	stages.deploy_VM_NET()
	
}

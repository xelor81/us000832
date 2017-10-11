node {
	env.WORKSPACE_ROOT_DIR = env.WORKSPACE.substring(0, env.WORKSPACE.lastIndexOf('/'));
	
	
	git_login='513094ca-73b7-4145-8829-4474bbbb1c24'
	git_url='git@github.com:xelor81/us000832.git'
	git_branch='master'
	
	//------------------ EXECUTE PIPELINES JOBS ---------------------------
	println("git URL:    " + git_url)
	println("git LOGIN:  " + git_login)
	println("git BRANCH: " + git_branch)
	println("VM VOL:     " + env.Host1)
	
	def host_params = env.Host1.split(',')
	
	println("VM PARAMS:" + host_params)
	
	println("NETWORK:  " + host_params[3:6])
	
	stage("***** Checkout Github *****") {
		git branch: git_branch, credentialsId: git_login, url: git_url
	}
	
	def stages = load "./pipeline/common_functions.groovy";
	
	
	stages.deploy_VM_NET()
	stages.deploy_VM_LVM(host_params[0])
	//stages.deploy_VM(host_params[0], host_params[1], host_params[2])
}

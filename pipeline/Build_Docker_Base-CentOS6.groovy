node {
    stage("GitHub Checkout of us000832") {
		git credentialsId: '513094ca-73b7-4145-8829-4474bbbb1c24', url: 'git@github.com:xelor81/us000832.git'
    }
	
	stage
}
    

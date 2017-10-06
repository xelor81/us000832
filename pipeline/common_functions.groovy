def deploy_VM_NET() {
	stage('Defining VM Networks and activating them') {
		ansiblePlaybook colorized: true,
		extras: 'NETWORK_NAME=Isolated1,Isolated2,Isolated3',
		installation: 'Ansible 2.1.2.0',
		playbook: './Ansible/Playbooks/VM/define-VM-net.yml',
		sudo: true
	}

}

def deploy_VM_LVM(VOL_NAME) {
	stage('Defining VM Storage') {
		ansiblePlaybook colorized: true,
		extras: 'VG_NAME=kvm-vms VOL_NAME=${VOL_NAME}',
		installation: 'Ansible 2.1.2.0',
		playbook: './Ansible/Playbooks/VM/define-VM_LVM.yml',
		sudo: true
	}

}


def deploy_VM_ans(VM_NAME, CPU, MEMORY, VM_NETWORK) {
	stage('Deploying VM') {
		ansiblePlaybook colorized: true, 
		extras: "CPU=2 MEMORY=1024 VM_STORAGE=/tmp/zz VM_NAME=desktop5 VM_NETWORK=bridge,Isolated2,Isolated3,Isolated1", 
		installation: 'Ansible 2.1.2.0', 
		playbook: './Ansible/Playbooks/VM/defineVM.yml', 
		sudo: true
	}

}

return this;

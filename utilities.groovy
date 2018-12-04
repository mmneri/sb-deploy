// ##################################################################################
//
//   Utility Functions
//
// ##################################################################################

public getBranchType(String branch_name) {
    //Must be specified according to <flowInitContext> configuration of jgitflow-maven-plugin in pom.xml
    def dev_pattern = ".*dev"
    def release_pattern = ".*release/.*"
    def feature_pattern = ".*feature/.*"
    def hotfix_pattern = ".*hotfix/.*"
    def master_pattern = ".*master"
    if (branch_name =~ dev_pattern) {
        return "dev"
    } else if (branch_name =~ release_pattern) {
        return "release"
    } else if (branch_name =~ master_pattern) {
        return "master"
    } else if (branch_name =~ feature_pattern) {
        return "feature"
    } else if (branch_name =~ hotfix_pattern) {
        return "hotfix"
    } else {
        return null;
    }
}

public getBranchDeploymentEnvironment(String branch_type) {
    if (branch_type == "dev") {
        return "dev"
    } else if (branch_type == "release") {
        return "staging"
    } else if (branch_type == "master") {
        return "prod"
    } else {
        return null;
    }
}

public getBranchName(){
    def branchName
    if(isUnix()){
        branchName = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
    } else {
        branchName = bat(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
    }
    return branchName
}

public mvn(String goals) {
    def mvnHome = tool "mvn"

    if (isUnix()) {
        sh "'${mvnHome}/bin/mvn' -B ${goals}"
    } else {
        bat(/"${mvnHome}\bin\mvn" -B ${goals}/)
    }
}

public cmd(String goals) {
    if (isUnix()) {
        sh "${goals}"
    } else {
        bat(/${goals}/)
    }
}

// ##################################################################################
//
//   Deploy Functions
//
// ##################################################################################

sleepDuration = 1

public compareVersions ( requiredVersions, currentVersions) {

    currentapps = currentVersions.stringPropertyNames().toArray()
    reqapps = requiredVersions.stringPropertyNames().toArray()
    Properties updatedVersions = new Properties()

    for (i=0; i < reqapps.size(); i++) {

        def app=reqapps[i]

        if (currentVersions.getProperty(app) == requiredVersions.getProperty(app) ) {
            log "Calculating Deltas", "Correct version of $app already deployed"
        } else {
            log "Calculating Deltas", "Adding $app for deployment"
            updatedVersions.setProperty(app, requiredVersions.getProperty(app))
        }
    }

    return updatedVersions
}

public getArtifact(app, revision) {	
	log ("getArtifact", """get Artifact steps here for app: $app""")
	copyArtifacts projectName: "${app}", filter: "target/*##*.war", target: "deploy/${app}/${revision}";    
}

public deploy(app, revision) {
    log ("Deploy", """Perform the deploy steps here for app: $app:$revision """)
    // def tc = TomcatAdapter( 
    //    url: "http://localhost:8181",
    //    credentialsId: "deploy"
    // )
    // deploy container: tc, war: "deploy/${app}/target/*.war", contextPath: "fff", onFailure: false;
    withCredentials([usernamePassword(credentialsId: 'deploy', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
    	cmd "curl http://$USERNAME:$PASSWORD@localhost:8181/manager/text/stop?path=/fff"
    	cmd "curl http://$USERNAME:$PASSWORD@localhost:8181/manager/text/undeploy?path=/fff"
	    cmd "curl --upload-file deploy/$app/$revision/target/fff.war http://$USERNAME:$PASSWORD@localhost:8181/manager/text/deploy?path=/fff"
	}
    
}

public performNFT() {
    log ("Run NFT",  "Perform the NFT steps")
}

public triggerRun() {
    log "Trigger build", "Triggering a new build"
    build job: env.JOB_NAME, propagate: false, wait: false
}

public getBlockedBuilds(fullName) {
    def q = Jenkins.instance.queue
    items = q.items
    Items[] matches = []
    for (hudson.model.Queue.Item item : items) {
        if (item.task.fullName==env.JOB_NAME) {
            log "Matched item", "matched item $item"
            matches.add( item)
        }
    }
    return matches
}

public writePropertiesFile(props, file) {
    log "WriteProperties", "File = $file"
    writeFile file: file, text: writeProperties(props)
}

@NonCPS public writeProperties (props) {
    def sw = new StringWriter()
    props.store(sw, null)
    return sw.toString()
}

public readPropertiesFromFile (file) {
    log "ReadProperties", "File = $file"
    def str = readFile file: file, charset : 'utf-8'
    def sr = new StringReader(str)
    def props = new Properties()
    props.load(sr)
    return props
}

public log (step, msg) {

    echo """************************************************************
            Step: $step
            $msg
            ************************************************************"""
}

return this;

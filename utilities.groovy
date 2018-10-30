// ##################################################################################
//
//   Utility Functions
//
// ##################################################################################

def get_branch_type(String branch_name) {
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

def get_branch_deployment_environment(String branch_type) {
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

def mvn(String goals) {
    def mvnHome = tool "mvn"

    if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' -B ${goals}"
	  } else {
	     bat(/"${mvnHome}\bin\mvn" -B ${goals}/)
	  }
}

def version() {
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    return matcher ? matcher[0][1] : null
}

// ##################################################################################
//
//   Deploy Functions
//
// ##################################################################################

def compareVersions ( requiredVersions, currentVersions) {

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


def decom(app, revision) {
    node ("$app-deploy-runner") {
        log ("Decomission", """Perform the decomission steps here for app: $app eg call sh /scripts/$app/decom nft""")
        sleep time: sleepDuration
    }
}

def deploy(app, revision) {
    node ("$app-deploy-runner") {
        log ("Deploy", """Perform the deploy steps here for app: $app:$revision eg call sh /scripts/$app/deploy nft $revision""")
        sleep time: sleepDuration
    }
}

def performNFT() {
    node ("nft-runner") {
        log ("Run NFT",  "Perform the NFT steps")
        sleep time: sleepDuration
    }
}

def triggerRun() {
    log "Trigger build", "Triggering a new build"
    build job: env.JOB_NAME, propagate: false, wait: false
}

def getBlockedBuilds(fullName) {
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

def writePropertiesFile(props, file) {
    log "WriteProperties", "File = $file"
    writeFile file: file, text: writeProperties(props)
}

@NonCPS def writeProperties (props) {
    def sw = new StringWriter()
    props.store(sw, null)
    return sw.toString()
}

def readPropertiesFromFile (file) {
    log "ReadProperties", "File = $file"
    def str = readFile file: file, charset : 'utf-8'
    def sr = new StringReader(str)
    def props = new Properties()
    props.load(sr)
    return props
}

def log (step, msg) {

    echo """************************************************************
			Step: $step
			$msg
			************************************************************"""
}
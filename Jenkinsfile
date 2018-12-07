// This script reads the manifest from an upstream job
// it compares with the last successful manifest from this build
// based on the differences, it triggers deploys in parallel
// On completion of the deploys it runs NFT
// On completion of NFT it triggers a new run
// Note multiple triggers with the same parameters are coalesced

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

sleepDuration = 15
manifestLocation = "sb-update-manifest"

def utilities
def move = "move"

stage('Reading Manifest'){
    node {
        git 'https://github.com/mmneri/sb-deploy.git'
      	utilities = load 'utilities.groovy' 
      	
      	def out = ""
      	withCredentials([usernamePassword(credentialsId: 'deploy', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
	    	utilities.cmd "curl -vs http://$USERNAME:$PASSWORD@localhost:8181/manager/text/list > pid"
            def s = readFile 'pid'
            out = s.trim()
	    } 
      	
      	utilities.log("OUTPUT", out)
      	
        step([$class: 'CopyArtifact', filter: 'manifest', projectName: manifestLocation, selector: [$class: 'StatusBuildSelector', stable: false]])
        if (isUnix()) {
		    move = "mv"
		}
        utilities.cmd("${move} manifest targetmanifest")
        requiredVersions = utilities.readPropertiesFromFile("targetmanifest")
    
        try {
            step([$class: 'CopyArtifact', filter: 'manifest', projectName:env.JOB_NAME, selector: [$class: 'StatusBuildSelector', stable: false]])
            utilities.cmd("${move} manifest currentmanifest")
            currentVersions = utilities.readPropertiesFromFile("currentmanifest")
        } catch (Exception e) {
            echo e.toString()
            currentVersions = new Properties()
        }
    }
}

stage('Determining Updated Apps'){
    node {
        updatedVersions = utilities.compareVersions( requiredVersions, currentVersions)

        appsToUpdate = updatedVersions.stringPropertyNames().toArray()
    }
}
    
stage('Updating Apps'){
    node {
    	
        if (appsToUpdate.size()>0) {
            utilities.log "Update Apps", "The following apps require updating: ${appsToUpdate.toString()}"
    
            def branches = [:]
            for (i=0; i < appsToUpdate.size(); i++) {
                def app = appsToUpdate[i]
                def revision = updatedVersions.getProperty(app)
                branches[app] = {
                    utilities.getArtifact(app, revision)
                    utilities.deploy (app, revision)
                }
            }
            parallel branches
        }
        utilities.writePropertiesFile(requiredVersions, "manifest")
        archive 'manifest'
        utilities.writePropertiesFile(updatedVersions, "updates")
        archive 'updates'
    }
}

stage concurrency: 1, name: 'Perform NFT'
    node{
        utilities.performNFT()
    }

stage("Check queue and re-trigger"){
    node{
        //utilities.triggerRun()
    }
    
}

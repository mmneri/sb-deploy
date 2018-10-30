// This script reads the manifest from an upstream job
// it compares with the last successful manifest from this build
// based on the differences, it triggers deploys in parallel
// On completion of the deploys it runs NFT
// On completion of NFT it triggers a new run
// Note multiple triggers with the same parameters are coalesced

sleepDuration = 15
manifestLocation = "sb-deploy"

def utilities

stage('Reading Manifest'){
    node {
        git 'https://github.com/mmneri/sb-deploy.git'
      	utilities = load 'utilities.groovy'  
      	
        step([$class: 'CopyArtifact', filter: 'manifest', projectName: manifestLocation, selector: [$class: 'StatusBuildSelector', stable: false]])
        sh "mv manifest targetmanifest"
        requiredVersions = utilities.readPropertiesFromFile("targetmanifest")
    
        try {
            step([$class: 'CopyArtifact', filter: 'manifest', projectName:env.JOB_NAME, selector: [$class: 'StatusBuildSelector', stable: false]])
            sh "mv manifest currentmanifest"
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
        checkpoint 'Starting App Update'
    
        if (appsToUpdate.size()>0) {
            log "Update Apps", "The following apps require updating: ${appsToUpdate.toString()}"
    
            def branches = [:]
            for (i=0; i < appsToUpdate.size(); i++) {
                def app=appsToUpdate[i]
                def revision = updatedVersions.getProperty(app)
                branches[app] = {
                    utilities.decom(app, revision)
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
        checkpoint 'Starting NFT'
        utilities.performNFT()
    }

stage("Check queue and re-trigger"){
    node{
        utilities.triggerRun()
    }
    
}





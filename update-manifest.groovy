// Application being triggered is supplied in the variable: app
// Revision is supplied in the variable: revision

def downstreamJob="sb-deploy"
def utilities

if (revision=="") {
    error 'No revision specified'
}

echo "Received update $app:$revision"


stage('Reading Manifest') {
	node {
		git 'https://github.com/mmneri/sb-deploy.git'
	  	utilities = load 'utilities.groovy'  
	  	
	    sh "echo 'reading manifest as an artifact'"
	    try {
	        step([$class: 'CopyArtifact', filter: 'manifest', projectName:env.JOB_NAME, selector: [$class: 'StatusBuildSelector', stable: false]])
	        versions = utilities.readPropertiesFromFile ("manifest")
	    } catch (Exception e) {
	        echo e.toString()
	        versions = new Properties()
	    }
    }
}
    
stage('Merging Manifest') {
	node {
		sh "echo 'Merging the manifest with the new versions'"
		versions[app] = revision
	}
}

stage('Writing Manifest') {
	node {
        utilities.writePropertiesFile(versions, "manifest")
        archive 'manifest'
    }
}

stage('Triggering Release Build') {
    log "Trigger build", "Triggering a new build"
    build job: downstreamJob, propagate: false, wait: false
}
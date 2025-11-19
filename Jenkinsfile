pipeline {
    agent none
    
    stages {
        stage('Build') {
            agent { label 'linux-jdk21' }

            steps {
                sh 'chmod +x gradlew'
                sh './gradlew shadowJar'
                script {
                    def sanitizedBranch = env.BRANCH_NAME.replaceAll(/[^a-zA-Z0-9._]/, '_')
                    def shortHash = env.GIT_COMMIT.substring(0, 6)
                    def jars = findFiles(glob: 'build/libs/BreweryX*.jar')
                    
                    jars.each { jar ->
                        def newPath = jar.path.replaceFirst(/\.jar$/, "-${sanitizedBranch}-${shortHash}.jar")
                        sh "mv '${jar.path}' '${newPath}'"
                    }
                }
            }

            post {        
                always {
                    archiveArtifacts artifacts: 'build/libs/BreweryX*.jar', fingerprint: true
                }
            }
        }
    }
}

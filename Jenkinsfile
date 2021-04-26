// The library version is controled from the Jenkins configuration
// To force a version add after lib '@' followed by the version.
@Library(['msaas-shared-lib', 'irp-obill-msaas-shared-lib']) _

node {
    // setup the global static configuration
    config = setupMsaasPipeline('msaas-config.yaml')
    //setting default oBill configs for CI pipeline
    oBillConfig = setupOBillPipeline()
}


pipeline {

    agent {
        kubernetes {
            label "${config.pod_label}"
            yamlFile 'KubernetesPods.yaml'
        }
    }

    post {
        always {
            sendMetrics(config)
        }
        fixed {
            emailext (
                subject: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' ${currentBuild.result}",
                body: """
                        Job Status for '${env.JOB_NAME} [${env.BUILD_NUMBER}]': ${currentBuild.result}\n\nCheck console output at ${env.BUILD_URL}
                """,
                to: oBillConfig.committerEmail
            )
        }
        unsuccessful {
            emailext (
                subject: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' ${currentBuild.result}",
                body: """
                        Job Status for '${env.JOB_NAME} [${env.BUILD_NUMBER}]': ${currentBuild.result}\n\nCheck console output at ${env.BUILD_URL}
                """,
                to: oBillConfig.committerEmail
            )
        }
    }

    stages {
        stage('BUILD:') {
            when { anyOf {  branch 'develop'; branch 'release';  branch 'regression'; branch 'master'; changeRequest() } }
            stages {
                stage('Get WEBS Config Version') {
                    steps {
                        script {
                            //get oBill Config version based on the CI triggered branch
                            oBillGetConfigVersion(oBillConfig)
                        }
                    }
                }
                stage('Docker Multi Stage Build') {
                    steps {
                        container('docker') {
                            cocoonInit(config)
                            // '--rm=false' is to not auto-delete intermediate contains so we can later do 'docker cp'
                            // '--build-arg=\"build=${BUILD_URL}\"' is to tag that intermediate container for same reason.
                            sh label: "docker build", script: "docker build --rm=false --build-arg=\"build=${env.BUILD_URL}\" --build-arg=\"websConfigVersion=${oBillConfig.websConfigVersion}\" -t ${config.image_full_name} ."

                            // copy from container the results of the build so we have all the reports.
                            // this is where Maven puts the results, for other builds change the path to copy.
                            sh label: "docker cp from build container to outputs", script: "docker cp \$(docker ps -l -a -q -f \"label=image=build\" -f \"label=build=${env.BUILD_URL}\" -f \"status=exited\"):/usr/src/app/target /var/run/outputs"

                            //here is an extra file that gets built in docker `build.params.json` that doesn’t get copied out into the pipeline. I use it to get some metadata about the build. We can recalculate it in jenkins build, but since its already there, we are simply copying it over
                            sh label: "docker cp package folder from build container to outputs", script: "docker cp \$(docker ps -l -a -q -f \"label=image=build\" -f \"label=build=${env.BUILD_URL}\" -f \"status=exited\"):/usr/src/package/target /var/run/outputs/package"
                        }
                            sh script: "cp -R /var/run/outputs \${WORKSPACE}/target"
                    }
                }
                stage('Code Scans') {
                    parallel {
                        stage('Code Analysis') {
                            when { expression {return config.SonarQubeAnalysis} }
                            steps {
                                container('docker') {

                                    // copy from container bundle for sonar analysis
                                    sh label: "docker cp from build container to outputs", script: "docker cp \$(docker ps -l -a -q -f \"label=image=build\" -f \"label=build=${env.BUILD_URL}\" -f \"status=exited\"):/usr/src /var/run/outputs/bundle"
                                    sh script: "cp -R /var/run/outputs/bundle \${WORKSPACE}/bundle"
                                    sh label: "docker build", script: "docker build -f Dockerfile.sonar --build-arg=\"sonar=${config.SonarQubeEnforce}\" ."
                                }
                            }
                        }

                        stage('checkmarx') {
                            steps {
                                checkmarx(config)
                            }
                        }
                        stage('Nexus IQ Server Scan') {
                            steps {
                            nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: "${config.asset_id}", iqStage: "build"
                            }
                        }
                    }
                }
                stage('Publish') {
                    parallel {
                        stage('Cocoon Publish') {
                            when { expression {return config.enableCocoon} }
                            steps {
                                container('docker') {
                                    cocoonPush(config)
                                }
                            }
                        }
                        stage('Report Coverage & Unit Test Results') {
                            steps {
                                junit '**/surefire-reports/**/*.xml'
                                jacoco()
                                codeCov(config)
                            }
                        }
                        stage('Version & CPD') {
                            options {
                                lock(resource: 'docker-version', inversePrecedence: true)
                                timeout(time: 22, unit: 'MINUTES')
                            }
                            stages {
                                stage('Get Build Version and Environments') {
                                    steps {
                                        //the last argument is the default envs you want it to deploy to if the release app returns 404 because it hasn’t been added yet
                                        oBillGetVersionAndEnvironments(oBillConfig, config, ['qal': ['qa2-usw2']])
                                    }
                                }
                                stage('CPD Certification & Publish') {
                                    steps {
                                        container('cpd') {
                                            cpd(config, "--buildargs BUILD_TAG=${config.git_tag} --buildargs DOCKER_TAGS='${oBillConfig.additional_docker_tags}'")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage('qal-usw2') {
                    when { allOf { anyOf { branch 'develop'; branch 'release'; branch 'regression'; branch 'master' }; not {changeRequest()} } }
                    stages {
                        stage('Milestone') {
                            steps {
                                milestone(ordinal: 10, label: "Deploy-qal-usw2-milestone")
                            }
                        }
                        stage('Deploys') {
                            parallel {
                                stage('qa1-usw2') {
                                    when { expression {return oBillConfig.deployEnvs?.get('qal')?.contains('qa1-usw2')} }
                                    stages {
                                        stage('Deploy') {
                                            options {
                                                lock(resource: getEnv(config, 'qa1-usw2').namespace, inversePrecedence: true)
                                                timeout(time: 22, unit: 'MINUTES')
                                            }
                                            steps {
                                                container('cdtools') {
                                                    gitOpsDeploy(config, "qa1-usw2", oBillConfig.version_image_name)
                                                }
                                            }
                                        }
                                    }
                                }
                                stage('qa2-usw2') {
                                    when { expression {return oBillConfig.deployEnvs?.get('qal')?.contains('qa2-usw2')} }
                                    stages {
                                        stage('Deploy') {
                                            options {
                                                lock(resource: getEnv(config, 'qa2-usw2').namespace, inversePrecedence: true)
                                                timeout(time: 22, unit: 'MINUTES')
                                            }
                                            steps {
                                                container('cdtools') {
                                                    gitOpsDeploy(config, "qa2-usw2", oBillConfig.version_image_name)
                                                }
                                            }
                                        }
                                    }
                                }
                                stage('qa3-usw2') {
                                    when { expression {return oBillConfig.deployEnvs?.get('qal')?.contains('qa3-usw2')} }
                                    stages {
                                        stage('Deploy') {
                                            options {
                                                lock(resource: getEnv(config, 'qa3-usw2').namespace, inversePrecedence: true)
                                                timeout(time: 22, unit: 'MINUTES')
                                            }
                                            steps {
                                                container('cdtools') {
                                                    gitOpsDeploy(config, "qa3-usw2", oBillConfig.version_image_name)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                //Sending data to release dashboard
                stage('Send Data to Dashboard') {
                    when { allOf { anyOf { branch 'develop'; branch 'release'; branch 'regression'; branch 'master' }; not {changeRequest()} } }
                    steps {
                        //send data back to the release app to be stored in its data store
                        oBillSendDataToDashboard(oBillConfig, config)
                    }
                }
            }
        }
    }
}

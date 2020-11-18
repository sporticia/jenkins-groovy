def lintDockerfile(PipelineState state) {
    def dockerLint = state.config.dockerLint ?: false
    docker.image('<ecr>/hadolint:1.18.0-6-ga0d655d-debian-20204410-134410').inside("-v ${env.AWS_WEB_IDENTITY_TOKEN_FILE}:${env.AWS_WEB_IDENTITY_TOKEN_FILE} -e AWS_WEB_IDENTITY_TOKEN_FILE=${env.AWS_WEB_IDENTITY_TOKEN_FILE} -e AWS_ROLE_ARN=${env.AWS_ROLE_ARN}") {
        withTempDir {
            sh(
                script: """#!/bin/bash
                    set -euo pipefail
                    # Check for a hadolint config override file
                    find \$WORKSPACE -iname '.hadolint.yaml' -exec cp -v '{}' . ';' || true
                    # Find and copy the Dockerfile to our current location
                    find \$WORKSPACE -iname '*Dockerfile' -exec cp -v '{}' . ';' || true
                    # Lint/parse our copy of the file
                    hadolint *Dockerfile && tee -a hadolint_lint.txt
                """
            )
        }
    }
}
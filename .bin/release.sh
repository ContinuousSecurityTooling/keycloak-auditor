#!/usr/bin/env bash
git checkout develop
./mvnw gitflow:release-start
version=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
# changelog config
cat << EOF > config.json
{
  "version": "$version"
}
EOF
conventional-changelog -p angular -i CHANGELOG.md -s -c config.json && \
 rm config.json
 git add CHANGELOG.md && \
 git commit -m "Changelog"
# Finish release
./mvnw gitflow:release-finish -DnoDeploy=true && \
 git push origin main && git push --tags && \
 git checkout develop

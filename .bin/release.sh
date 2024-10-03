#!/usr/bin/env bash
git checkout develop
./mvnw release:prepare
version=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
(cd sdk && npm version $version && npm i && git add package.json package-lock.json package.json)
git commit -m "chore: Updating SDK to $version"
# changelog config
cat <<EOF >target/config.json
{
  "version": "$version"
}
EOF
conventional-changelog -p angular -i CHANGELOG.md -s -c target/config.json
git add CHANGELOG.md &&
  git commit -m "chore: Changelog for $version"
# Finish release
./mvnw release:prepare release:perform -B -DskipITs=true -Darguments="-DskipTests=true -DskipITs=true"

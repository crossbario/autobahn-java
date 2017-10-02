#!/bin/sh

set -e

echo '# Release' $(git describe --abbrev=0)'\n' >> CHANGELOG.md
git log $(git describe --abbrev=0)..HEAD --pretty=format:"  * %s" >> CHANGELOG.md
echo '\n\n' >> CHANGELOG.md


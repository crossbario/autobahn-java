#!/bin/sh

set -e

echo '# Release' $(git describe --abbrev=0)'\n' >> CHANGELOG
git log $(git describe --abbrev=0)..HEAD --pretty=format:"  * %s" >> CHANGELOG
echo '\n\n' >> CHANGELOG


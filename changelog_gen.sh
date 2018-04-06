#!/bin/sh

set -e

FILE=$(mktemp)
TAG_CURRENT=$(git describe --abbrev=0)
TAG_PREVIOUS=$(git describe --abbrev=0 --tags $(git rev-list --tags --skip=1 --max-count=1))
echo '# Release' $TAG_CURRENT'\n' >> $FILE
git log $TAG_PREVIOUS..$TAG_CURRENT --pretty=format:"* %s" >> $FILE
echo '\n\n---\n' >> $FILE
cat CHANGELOG.md >> $FILE
cp $FILE CHANGELOG.md
rm $FILE


#!/bin/sh

set -e

FILE=$(mktemp)
TAG_CURRENT=$(git describe --abbrev=0)
echo '# Release' $TAG_CURRENT'\n' >> $FILE
git log $TAG_CURRENT...HEAD --pretty=format:"* %s" >> $FILE
echo '\n\n---\n' >> $FILE
cat CHANGELOG.md >> $FILE
cp $FILE CHANGELOG.md
rm $FILE


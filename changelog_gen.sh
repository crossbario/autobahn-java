#!/bin/sh

set -e

FILE=$(mktemp)

echo '# Release' $(git describe --abbrev=0)'\n' >> $FILE
git log $(git describe --abbrev=0)..HEAD --pretty=format:"  * %s" >> $FILE
echo '\n\n' >> $FILE
cat CHANGELOG.md >> $FILE
cp $FILE CHANGELOG.md
rm $FILE


#!/bin/bash

ARTIFACT_ID=$1
VERSION=$2
NIX_VERSION=$3

# Copy the external dependency
rm -rf "target/jobson-deb-${VERSION}"
mkdir -p "target/jobson-deb-${VERSION}/usr"
cp -r target/dependencies/jobson-nix-${NIX_VERSION}/* "target/jobson-deb-${VERSION}/usr"
cd "target/jobson-deb-${VERSION}/usr"


# Move jars into jobson /share folder because we can't risk clobbering debian's jars
mv share/java share/jobson

cat <<EOF > bin/jobson
#!/bin/bash

java -jar '/usr/share/jobson/java/jobson-$VERSION.jar' "\$@"
EOF
chmod 755 bin/jobson

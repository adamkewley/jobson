#!/bin/bash

PROJECT_BUILD_DIR=$1
ARTIFACT_ID=$2
VERSION=$3

OUTDIR=${PROJECT_BUILD_DIR}/${ARTIFACT_ID}-${VERSION}

mkdir -p "${OUTDIR}/share/java"
cp -r ${PROJECT_BUILD_DIR}/dependencies/jobson/* ${OUTDIR}/share/java

mkdir -p ${OUTDIR}/share/jobson/ui/html
cp -r ${PROJECT_BUILD_DIR}/dependencies/jobson-ui/jobson-ui-${VERSION}/* ${OUTDIR}/share/jobson/ui/html

mkdir -p  ${OUTDIR}/share/doc/jobson/html
cp -r ${PROJECT_BUILD_DIR}/dependencies/jobson-docs/jobson-docs-${VERSION}/* ${OUTDIR}/share/doc/jobson/html

mkdir -p ${OUTDIR}/bin
cp src/jobson ${OUTDIR}/bin/jobson
sed -i "s/JOBSON_VERSION/${VERSION}/" ${OUTDIR}/bin/jobson
chmod +x ${OUTDIR}/bin/jobson

cd ${PROJECT_BUILD_DIR} && tar czf ${ARTIFACT_ID}-${VERSION}.tar.gz ${ARTIFACT_ID}-${VERSION}
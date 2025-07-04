#!/usr/bin/env bash

LIC="license.xml"

if stat "$LIC" &>/dev/null; then
  echo "File '$LIC' exists."
else
    docker container rm -f liferay-dxp-latest && \
        docker create --pull always --name liferay-dxp-latest liferay/dxp:latest && \
        docker export liferay-dxp-latest | tar -xv --strip-components=3 --wildcards -C . opt/liferay/deploy/*.xml && \
        mv trial-dxp-license*.xml license.xml
fi




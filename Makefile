.ONESHELL:
SHELL = bash

.DEFAULT_GOAL = help
CLUSTER_NAME := ep25cx
DXP_IMAGE_TAG := 7.4.13-u132
LOCAL_MOUNT := k3d-tmp/mnt/local

### TARGETS ###

clean: delete-cluster clean-dxp-modules clean-local-mount ## Clean up everything

clean-dxp-modules: ## Clean DXP modules
	@cd ./topics-exchange-workspace/ && ./gradlew clean
	@rm -rf "${PWD}/${LOCAL_MOUNT}/osgi/"

clean-license:
	@rm -f license.xml

clean-local-mount: ## Create k3d local mount folder
	@rm -rf "${PWD}/${LOCAL_MOUNT}/*"

delete-cluster: ## Delete k3d cluster
	@k3d cluster delete "${CLUSTER_NAME}" || true

copy-dxp-modules-to-local-mount: dxp-modules ## Copy DXP modulesd to local mount
	@mkdir -p "${PWD}/${LOCAL_MOUNT}/osgi/modules/"
	@cp -fv ./topics-exchange-workspace/bundles/osgi/modules/* "${PWD}/${LOCAL_MOUNT}/osgi/modules/"

dxp-modules: ## Build DXP Modules
	@cd ./topics-exchange-workspace/ && ./gradlew clean build deploy -x test -x check

help:
	@grep -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
	
license:
	@docker container rm -f liferay-dxp-latest && \
		docker create --pull always --name liferay-dxp-latest liferay/dxp:latest && \
		docker export liferay-dxp-latest | tar -xv --strip-components=3 --wildcards -C . opt/liferay/deploy/*.xml && \
		mv trial-dxp-license*.xml license.xml

mkdir-local-mount: ## Create k3d local mount folder
	@mkdir -p "${PWD}/${LOCAL_MOUNT}"

deploy-dxp: copy-dxp-modules-to-local-mount ## Deploy DXP and sidecars into cluster (Make sure you 'make start-cluster' first)
	@helm upgrade -i liferay \
		oci://us-central1-docker.pkg.dev/liferay-artifact-registry/liferay-helm-chart/liferay-default \
		--create-namespace \
		--namespace liferay-system \
		--set "image.tag=${DXP_IMAGE_TAG}" \
		--set-file "configmap.data.license\.xml=license.xml" \
		-f helm-values/values.yaml

undeploy-dxp:
	@helm uninstall -n liferay-system liferay

delete-pvc:
	@kubectl delete pvc --selector "app.kubernetes.io/name=liferay-default" -n liferay-system

start-cluster: mkdir-local-mount ## Start k3d cluster
	@k3d cluster create "${CLUSTER_NAME}" \
		--port 80:80@loadbalancer \
		--registry-create registry:5000 \
		--volume "${PWD}/${LOCAL_MOUNT}:/mnt/local@all:*"


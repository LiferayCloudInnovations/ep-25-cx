.ONESHELL:
SHELL = bash

.DEFAULT_GOAL = help
CLUSTER_NAME := ep25cx
DXP_IMAGE_TAG := 7.4.13-u132
LOCAL_MOUNT := tmp/mnt/local

### TARGETS ###

clean: delete-cluster clean-dxp-modules clean-local-mount ## Clean up everything

clean-client-extensions: ## Clean Client Extensions
	@cd ./ep25cx-workspace/ && ./gradlew :client-extensions:clean
	@rm -rf "${PWD}/${LOCAL_MOUNT}/osgi/client-extensions"

clean-dxp-modules: ## Clean DXP modules
	@cd ./ep25cx-workspace/ && ./gradlew :modules:clean
	@rm -rf "${PWD}/${LOCAL_MOUNT}/osgi/modules"

clean-license:
	@rm -f license.xml

clean-local-mount: ## Create k3d local mount folder
	@rm -rf "${PWD}/${LOCAL_MOUNT}/*"

delete-cluster: ## Delete k3d cluster
	@k3d cluster delete "${CLUSTER_NAME}" || true

copy-client-extensions-to-local-mount: client-extensions## Copy client extensions to local mount
	@mkdir -p "${PWD}/${LOCAL_MOUNT}/osgi/client-extensions"
	@cp -fv ./ep25cx-workspace/bundles/osgi/client-extensions/* "${PWD}/${LOCAL_MOUNT}/osgi/client-extensions"

copy-dxp-modules-to-local-mount: dxp-modules ## Copy DXP modulesd to local mount
	@mkdir -p "${PWD}/${LOCAL_MOUNT}/osgi/modules"
	@cp -fv ./ep25cx-workspace/bundles/osgi/modules/* "${PWD}/${LOCAL_MOUNT}/osgi/modules"

client-extensions: clean-client-extensions ## Build Client Extensions
	@cd ./ep25cx-workspace/ && ./gradlew :client-extensions:build :client-extensions:deploy -x test -x check

dxp-modules: clean-dxp-modules ## Build DXP Modules
	@cd ./ep25cx-workspace/ && ./gradlew :modules:build :modules:deploy -x test -x check

hot-deploy-modules: copy-dxp-modules-to-local-mount switch-context ## Build and Copy DXP modules into running container
	@./bin/kubectl_copy_all "${PWD}/${LOCAL_MOUNT}/osgi/modules" liferay-default-0 /opt/liferay/osgi/modules liferay-system

help:
	@grep -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
	
license:
	@docker container rm -f liferay-dxp-latest && \
		docker create --pull always --name liferay-dxp-latest liferay/dxp:latest && \
		docker export liferay-dxp-latest | tar -xv --strip-components=3 --wildcards -C . opt/liferay/deploy/*.xml && \
		mv trial-dxp-license*.xml license.xml

mkdir-local-mount: ## Create k3d local mount folder
	@mkdir -p "${PWD}/${LOCAL_MOUNT}"

deploy-dxp: copy-dxp-modules-to-local-mount switch-context ## Deploy DXP and sidecars into cluster (Make sure you 'make start-cluster' first)
	@helm upgrade -i liferay \
		oci://us-central1-docker.pkg.dev/liferay-artifact-registry/liferay-helm-chart/liferay-default \
		--create-namespace \
		--namespace liferay-system \
		--set "image.tag=${DXP_IMAGE_TAG}" \
		--set-file "configmap.data.license\.xml=license.xml" \
		-f helm-values/values.yaml

deploy-client-extensions: copy-client-extensions-to-local-mount patch-coredns ## Deploy Client extensions to cluster
	@./bin/deploy_client_extensions "${PWD}/${LOCAL_MOUNT}/osgi/client-extensions"

clean-cx: switch-context ## Clean up Client Extensions
	@helm uninstall -n liferay-system $(helm list -n liferay-system -q --filter '-cx$')

clean-dxp: switch-context ## Clean up DXP deployment
	@helm uninstall -n liferay-system liferay

clean-data: switch-context ## Clean up data in the cluster
	@kubectl delete pvc --selector "app.kubernetes.io/name=liferay-default" -n liferay-system

patch-coredns: switch-context ## Patch CoreDNS to resolve hostnames
	@kubectl get cm coredns -n kube-system -o yaml | tee tmp.yaml | sed '/.*host.k3d.internal/ { p; s/host.k3d.internal/main.dxp.localtest.me/; }' | kubectl apply -f - && kubectl rollout restart deployment coredns -n kube-system

switch-context: ## Switch kubectl context to k3d cluster
	@kubectx k3d-${CLUSTER_NAME}

start-cluster: mkdir-local-mount ## Start k3d cluster
	@k3d cluster create "${CLUSTER_NAME}" \
		--port 80:80@loadbalancer \
		--registry-create registry:5000 \
		--volume "${PWD}/${LOCAL_MOUNT}:/mnt/local@all:*"


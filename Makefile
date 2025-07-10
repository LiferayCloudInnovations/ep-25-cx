.ONESHELL:
SHELL = bash

.DEFAULT_GOAL = help
CLUSTER_NAME := ep25cx
DXP_IMAGE_TAG := 7.4.13-u132
LOCAL_MOUNT := tmp/mnt/local

### TARGETS ###

clean: clean-cluster clean-dxp-modules clean-tmp ## Clean up everything

clean-cluster: ## Delete k3d cluster
	@k3d cluster delete "${CLUSTER_NAME}" || true

clean-data: switch-context undeploy-dxp ## Clean up data in the cluster
	@kubectl delete pvc --selector "app.kubernetes.io/name=liferay-default" -n liferay-system

clean-dxp-modules: ## Clean DXP modules
	@cd ./ep25cx-workspace/ && ./gradlew :modules:clean
	@rm -rf "${PWD}/${LOCAL_MOUNT}/osgi/modules"

clean-license:
	@rm -f license.xml

clean-tmp: ## Create k3d tmp folder
	@rm -rf "${PWD}/tmp"

copy-dxp-modules-to-local-mount: dxp-modules ## Copy DXP modulesd to local mount
	@mkdir -p "${PWD}/${LOCAL_MOUNT}/osgi/modules"
	@cp -fv ./ep25cx-workspace/bundles/osgi/modules/* "${PWD}/${LOCAL_MOUNT}/osgi/modules"

deploy: deploy-dxp deploy-cx ## Deploy DXP and Client Extensions to cluster (Make sure you 'make start-cluster' first)

deploy-cx: switch-context ## Deploy Client extensions to cluster
	@cd ./ep25cx-workspace/ && ./gradlew :client-extensions:helmDeploy -x test -x check

deploy-dxp: copy-dxp-modules-to-local-mount license patch-coredns switch-context ## Deploy DXP and sidecars into cluster (Make sure you 'make start-cluster' first)
	@helm upgrade -i liferay \
		oci://us-central1-docker.pkg.dev/liferay-artifact-registry/liferay-helm-chart/liferay-default \
		-f helm-values/values.yaml \
		--create-namespace \
		--namespace liferay-system \
		--set "image.tag=${DXP_IMAGE_TAG}" \
		--set-file "configmap.data.license\.xml=license.xml" \
		--timeout 10m \
		--wait
	@kubectl apply -f ./helm-values/websocket-services.yaml

dxp-modules: clean-dxp-modules ## Build DXP Modules
	@cd ./ep25cx-workspace/ && ./gradlew :modules:build :modules:deploy -x test -x check

help:
	@grep -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

hot-deploy-dxp-modules: copy-dxp-modules-to-local-mount switch-context ## Build and Copy DXP modules into running container
	@./bin/kubectl_copy_all "${PWD}/${LOCAL_MOUNT}/osgi/modules" liferay-default-0 /opt/liferay/osgi/modules liferay-system

license:
	@./bin/extract_license

mkdir-local-mount: ## Create k3d local mount folder
	@mkdir -p "${PWD}/${LOCAL_MOUNT}"

patch-coredns: switch-context ## Patch CoreDNS to resolve hostnames
	@./bin/patch_coredns
	@kubectl rollout restart deployment coredns -n kube-system

start-cluster: mkdir-local-mount ## Start k3d cluster
	@k3d cluster create "${CLUSTER_NAME}" \
		--port 80:80@loadbalancer \
		--port 15674:15674@loadbalancer \
		--port 15675:15675@loadbalancer \
		--registry-create registry:5000 \
		--volume "${PWD}/${LOCAL_MOUNT}:/mnt/local@all:*"
	@helm upgrade -i ksgate \
		oci://ghcr.io/ksgate/charts/ksgate \
		--create-namespace \
		--namespace ksgate-system \
		--timeout 5m \
		--wait

switch-context: ## Switch kubectl context to k3d cluster
	@kubectx k3d-${CLUSTER_NAME}

undeploy: undeploy-cx undeploy-dxp ## Clean up DXP and Client Extensions

undeploy-cx: switch-context ## Clean up Client Extensions
	@helm list -n liferay-system -q --filter "-cx" | xargs -r helm uninstall -n liferay-system
	@kubectl -n liferay-system delete cm --selector "lxc.liferay.com/metadataType=ext-init"

undeploy-dxp: switch-context ## Clean up DXP deployment
	@helm list -n liferay-system -q --filter "liferay" | xargs -r helm uninstall -n liferay-system
	@kubectl -n liferay-system delete cm --selector "lxc.liferay.com/metadataType=dxp"

# ep-25-cx

### Tools required

- Docker
- Kubectl
- a local Kubernetes impl (K3d is documented here)
- Helm
- Stern (optional but useful)
- a Liferay developer license

### Running Liferay with RabbitMQ using the official Helm chart

1. Setup a Kubernetes cluster if you don't have one:

   ```shell
   k3d cluster create playground \
     --port 80:80@loadbalancer \
     --registry-create registry:5000 \
     --volume "/home/me/tmp/mount:/mnt/local@all:*"
   ```

1. Make sure to place your license file at `license.xml` in the repo (it is ignored by git but needed by Helm chart).

1. Using the values file `helm-values/values.yaml` execute the following commands:

   ```shell
   helm upgrade -i liferay \
     oci://us-central1-docker.pkg.dev/liferay-artifact-registry/liferay-helm-chart/liferay-default \
     --create-namespace \
     --namespace liferay-system \
     --set "image.tag=7.4.13.nightly" \
     --set-file "configmap.data.license\.xml=license.xml" \
     -f helm-values/values.yaml
   ```

#### Reset from Scratch

If you need to delete everything deployed into the cluster and start again from scratch (also deleting any data) use the following commands:

```shell
helm -n liferay-system delete liferay
kubectl -n liferay-system delete pvc --selector "app.kubernetes.io/name=liferay-default"
```

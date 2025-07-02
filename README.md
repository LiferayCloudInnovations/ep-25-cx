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

### Make REST requests to RabbitMQ using Curl

A RabbitMQ user is setup in the Helm chart that allows making management requests over REST.

Start by running the following a container with curl as follows:

```shell
k run -it --rm --restart=Never alpine --image=alpine -- sh -c "apk add bash curl jq && bash"
```

At the prompt use `curl` to can make queries using the user credentials `lfrrabbitmq:lfrrabbitmq`.

e.g.

```shell
curl -sL -u lfrrabbitmq:lfrrabbitmq -H "Accept: application/json" http://liferay-default-rabbitmq.liferay-system:15672/api/overview?disable_stats=true | jq

curl -sL -u lfrrabbitmq:lfrrabbitmq -H "Accept: application/json" http://liferay-default-rabbitmq.liferay-system:15672/api/vhosts/%2f/channels | jq
```

The RabbitMQ REST API is documented [here](https://www.rabbitmq.com/docs/http-api-reference#overview).

#### Reset from Scratch

If you need to delete everything deployed into the cluster and start again from scratch (also deleting any data) use the following commands:

```shell
helm -n liferay-system delete liferay
kubectl -n liferay-system delete pvc --selector "app.kubernetes.io/name=liferay-default"
```

# ep-25-cx

## Tools required

- docker
- kubectl
- kubectx
- k3d
- helm
- Liferay developer license
- stern (optional but useful)
- make (optional but useful)

## Running Liferay with RabbitMQ using the official Helm chart

1. Setup a Kubernetes cluster if you don't have one:

   ```shell
   k3d cluster create playground \
     --port 80:80@loadbalancer \
     --registry-create registry:5000 \
     --volume "/home/me/tmp/mount:/mnt/local@all:*"
   ```

1. Make sure to place your license file at `license.xml` in the repo (it is
   ignored by git but needed by Helm chart).

1. Using the values file `helm-values/values.yaml` execute the following
   commands:

   ```shell
   helm upgrade -i liferay \
     oci://us-central1-docker.pkg.dev/liferay-artifact-registry/liferay-helm-chart/liferay-default \
     --create-namespace \
     --namespace liferay-system \
     --set "image.tag=7.4.13.nightly" \
     --set-file "configmap.data.license\.xml=license.xml" \
     -f helm-values/values.yaml
   ```

## Make REST requests to RabbitMQ using Curl

A RabbitMQ user is setup in the Helm chart that allows making management
requests over REST.

Start by running the following a container with curl as follows:

```shell
kubectl run -it --rm --restart=Never alpine --image=alpine -- sh -c "apk add bash curl jq && bash"
```

At the prompt use `curl` to can make queries using the user credentials
`lfrrabbitmq:lfrrabbitmq`.

e.g.

```shell
curl -sL -u lfrrabbitmq:lfrrabbitmq -H "Accept: application/json" http://liferay-default-rabbitmq.liferay-system:15672/api/overview?disable_stats=true | jq

curl -sL -u lfrrabbitmq:lfrrabbitmq -H "Accept: application/json" http://liferay-default-rabbitmq.liferay-system:15672/api/vhosts/%2f/channels | jq
```

The RabbitMQ REST API is documented
[here](https://www.rabbitmq.com/docs/http-api-reference#overview).

## Reset from Scratch

If you need to delete everything deployed into the cluster and start again from
scratch (also deleting any data) use the following commands:

```shell
helm -n liferay-system delete liferay
kubectl -n liferay-system delete pvc --selector "app.kubernetes.io/name=liferay-default"
```

## Use the Make file

### Hello Event Queue World Demo Script using `make`

1. Start K3d cluster

   ```shell
   make start-cluster
   ```

1. Create local developer license (or if you have your own license copy it to
   file named `license.xml`)

   ```shell
   make license
   ```

1. Deploy DXP to Cluster

   ```shell
   make deploy-dxp
   ```

1. View Liferay logs

   ```shell
   stern pod/liferay-default-0 -c liferay-default
   ```

1. [**Login** to DXP](http://main.dxp.localtest.me)
1. To get the password use

   ```shell
   kubectl get secrets liferay-default -o jsonpath='{.data.LIFERAY_DEFAULT_PERIOD_ADMIN_PERIOD_PASSWORD}' | base64 -d && echo
   ```

1. **View** the message broker's
   [queue dashboard](http://rabbitmq.localtest.me/#/queues)
1. Notice that there are NO object events queued
1. After logging in, deploy Client Extensions `make deploy-cx`
1. You should see the `EP25 Sample` object definition get added to Liferay
1. Add/Update/Remove several `EP25 Sample` objects in the objects content page
1. View message broker [queue dashboard](http://rabbitmq.localtest.me/#/queues)
   and you should
   [see a queue](http://rabbitmq.localtest.me/#/queues/%2F/C_EP25SampleEvent)
   for the object `EP25 Sample`
1. Click the `Get Messages` after putting in the number of events you wish to
   display

### Bring Demo Online with "One Shot"

```shell
make start-cluster deploy-dxp deploy-cx
```

### Undeploy DXP and Client Extensions

```shell
make undeploy-cx undeploy-dxp
```

### Clean Up Everything

```shell
make clean
```

## Debug Liferay

Liferay is already configured in debug mode.

Port forward to Liferay Pod's debug port:

```shell
kubectl -n liferay-system port-forward liferay-default-0 8000:8000
```

Connect using any IDE debugger to remote Java process at `localhost:8000`.

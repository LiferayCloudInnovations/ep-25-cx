# TODO

## Must Haves

-
-
-

## Nice to Haves

- fix /temp/liferay/deploy copy operation to use /temp/liferay/osgi/modules
- delay k8s agent until scopes are hydrated (can remove the helm upgrade --wait && curl in Makefile)

## Bugs

- fix rabbitmq missing probes
    - https://github.com/LiferayCloudInnovations/ep-25-cx/blob/main/helm-values/values.yaml#L313-L319
    - https://github.com/LiferayCloudInnovations/ep-25-cx/blob/main/helm-values/values.yaml#L330-L336
    - https://github.com/LiferayCloudInnovations/ep-25-cx/blob/main/helm-values/values.yaml#L362-L369
- module logs don't show up in liferay container logs

## Wont Fix / Known Issues

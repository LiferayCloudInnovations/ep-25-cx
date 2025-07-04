# TODO

## Must Haves

-
-
-

## Nice to Haves

- fix /temp/liferay/deploy copy operation to use /temp/liferay/osgi/modules
- delay k8s agent until scopes are hydrated (can remove the helm upgrade --wait && curl in Makefile)
- change the readiness probes of Liferay to hit the /o/api path

## Bugs

- module logs don't show up in liferay container logs

## Wont Fix / Known Issues

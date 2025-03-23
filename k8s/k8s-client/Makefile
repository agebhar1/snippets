registry := $(shell /home/agebhar1/bin/minikube ip):5000

.PHONY: all
all: build push delete run

.PHONY: build
build:
	podman build . --tag $(registry)/demo:latest

.PHONY: push
push:
	podman push $(registry)/demo:latest

.PHONY: delete
delete:
	-kubectl delete pod/demo

.PHONY: run
run:
	kubectl run demo --image registry.minikube/demo

.PHONY: logs
logs:
	kubectl logs -f demo
#!/usr/bin/env sh
# https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/

export KUBECONFIG=~/.kube/config

APISERVER=$(kubectl config view --minify | grep server | cut -f 2- -d ":" | tr -d " ")
SECRET_NAME=$(kubectl get secrets | grep ^default | cut -f1 -d ' ')
TOKEN=$(kubectl describe secret $SECRET_NAME | grep -E '^token' | cut -f2 -d':' | tr -d " ")

echo "TOKEN: ${TOKEN}"

curl $APISERVER/api --header "Authorization: Bearer $TOKEN" --insecure

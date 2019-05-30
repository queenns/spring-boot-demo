#!/usr/bin/env bash
#本地合并多环境
KUBECONFIG=/config/kube-dev.conf:/config/kube-prod.conf kubectl config view --flatten
#更新镜像
kubectl set image deployment/nvwa-auth nvwa-auth=10.32.40.44:8080/ad/nvwa-auth:9f97bcee-dev -n ad-dev
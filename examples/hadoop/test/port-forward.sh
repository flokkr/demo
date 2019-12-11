kubectl get po --selector=app=hadoop,service=test -o=custom-columns=:metadata.name --no-headers | xargs -IPOD kubectl port-forward POD 8000:8000 &

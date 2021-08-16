kubectl create namespace jiffy
kubectl create namespace temporal
helm install functionator functionator/
kubectl get pods --namespace=jiffy-automate
kubectl get svc --namespace=jiffy-automate
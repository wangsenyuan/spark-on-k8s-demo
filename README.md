## start minikube locally to test
```docker
export https_proxy=http://127.0.0.1:33210 http_proxy=http://127.0.0.1:33210 all_proxy=socks5://127.0.0.1:33211
export NO_PROXY=localhost,127.0.0.1,10.96.0.0/12,192.168.59.0/24,192.168.49.0/24,192.168.39.0/24
minikube start --cpus 4 --mount --mount-string="/Users/wangsenyuan/git/github/spark-sql-demo/spark-logs:/Users/wangsenyuan/git/github/spark-sql-demo/spark-logs"
```

## set docker env to make sure docker build pushed to minikube

```
eval $(minikube -p minikube docker-env)
```

## create kubernates spark-operator namespace & account
```kubernetes helm
kubectl apply -f spark-operator.yaml
```

## install helm and using helm to install spark-operator
```kubernetes helm
brew install helm

helm repo add spark-operator https://googlecloudplatform.github.io/spark-on-k8s-operator

helm install sparkoperator spark-operator/spark-operator --namespace spark-operator --set sparkJobNamespace=spark-apps --set webhook.enable=true --create-namespace
```


## docker build spark base image from spark source file
```docker
git clone git@github.com:apache/spark.git
cd spark
export MAVEN_OPTS="-Xss128m -Xmx2g -XX:ReservedCodeCacheSize=1g"
dev/make-distribution.sh -Pkubernetes -Phadoop -Dhadoop.version=3.3.2 -Phive
cd dist
docker build -t spark:latest -f kubernetes/dockerfiles/spark/Dockerfile .

```

## build docker app image based on spark-operator

### set up spark-history-server on k8s
0. build spark-history-server:latest docker image
```
    docker build -t spark-history-server:latest -f Dockerfile.history.server .
```
1. deploy spark-history-server
```kubernetes helm
    kubectl apply -f spark-history-server.yaml
```
2. port-forward spark-history-server pod to make it accessable
```kubernetes helm
    kubectl port-forward spark-history-server-c577cd669-bdhjj 18080:18080 -n spark-apps
```
### build example apps

0. sbt build jar
```docker
sbt clean assembly
```
1. docker build by Dockerfile (based on spark-operator)
```docker
docker build -t spark-sql-demo:latest -f Dockerfile .
```
2. run spark-pi example 
```docker
kubectl apply -f spark-pi.yaml
```

3. run spark-sql example
```kubernetes helm
kubectl apply -f spark-sql.yaml
```
4. run spark-streaming example
   1. run nc locally
```kubernetes helm
    nc -lk 9999
```
   2. run spark-streaming
```kubernetes helm
    kubectl apply -f spark-streaming.yaml
```

## build app by spark docker-image-tool to use spark-submit
1. assembly jar file

```scala
sbt clean assembly
```
2. step2 docker build image

```docker
docker build -t spark-sql-demo:latest -f Dockerfile.spark .
```

3. setup default namespace account & role bindings

```kubernetes helm
kubectl cluster-info
```

```kubernetes helm
kubectl create serviceaccount sparkx
```

```kubernetes helm
kubectl create clusterrolebinding sparkx-role --clusterrole=edit --serviceaccount=default:sparkx --namespace=default

```
4. run examples

- 1 pi example
```docker
$SPARK_HOME/bin/spark-submit \
    --master k8s://https://127.0.0.1:52518 \
    --deploy-mode cluster \
    --name spark-sql-demo \
    --class com.wsy.demo.pi.Main \
    --conf spark.executor.instances=3 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=sparkx \
    --conf spark.kubernetes.container.image=spark-sql-demo:latest \
    local:///app/spark-sql-demo.jar
```
- 2 sql example
```docker
$SPARK_HOME/bin/spark-submit \
    --master k8s://https://127.0.0.1:52518 \
    --deploy-mode cluster \
    --name spark-sql-demo \
    --class com.wsy.demo.sql.Main \
    --conf spark.executor.instances=3 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=sparkx \
    --conf spark.kubernetes.container.image=spark-sql-demo:latest \
    local:///app/spark-sql-demo.jar
```
- 3 streaming example
```kubernetes helm
kubectl apply -f spark-streaming-job.yaml 
```

## run spark-history server in k8s <https://stackoverflow.com/questions/51798927/spark-ui-history-server-on-kubernetes>

- 0 inorder to make local file system exist in the minikube node
```docker
minikube mount /Users/wangsenyuan/tmp/spark-logs:/Users/wangsenyuan/tmp/spark-logs
```
- 1 build persistent volume claim and deploy spark history server by spark-operator
```docker
kubectl apply -f spark-history-server.yaml
```
- 3 configure spark application to log events
- 4 port forward to view the page
```
kubectl port-forward spark-history-server-c577cd669-7f84h 18080:18080

```

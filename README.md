# Confluent for Kubernetes Workshop

In this workshop, you will:

- Launch your own Minikube for a local development environment and access an existing Tanzu Kubernetes Grid for production
- Deploy applications and the Confluent for Kubernetes platform for data in motion across both environments
- Easily mirror data from the production environment to the development environment
- Manage infrastructure declaratively and use GitOps to safely update applications
-See how Tanzu Mission Control is used as a global control plane across both development and production Kubernetes environments

## The Lab Environment

Your lab environment is an Ubuntu 18.04 server with all pre-requisites installed.

## Start minikube

Your development environment will consist of a functionally complete Confluent Platform deployed to minikube.

Minikube is a single node Kubernetes cluster.

Start the minikube cluster:

```
minikube start
```

View status:

```
minikube status
```

## Get project

```
cd code

git clone https://github.com/confluentinc/cfk-workshop.git
```

## Deploy CFK onto minikube

Deploy Confluent for Kubernetes

```
# Create namespace to use
kubectl create ns confluent

# Set namespace for current context to `confluent`
kubectl config set-context --current --namespace confluent

# Check your kubectl context
kubectl config get-contexts

# Add Helm repository
helm repo add confluentinc https://packages.confluent.io/helm

# Install CFK Operator
helm install cfk-operator confluentinc/confluent-for-kubernetes -n confluent

helm list -n confluent
NAME        	NAMESPACE	REVISION	STATUS  	CHART                            	APP VERSION
cfk-operator	confluent	1       	updated     confluent-for-kubernetes-0.174.13	2.0.1

kubectl get pods -n confluent
NAME                                  READY   STATUS    RESTARTS   AGE
confluent-operator-66bcf88444-vd5gg   1/1     Running   0          14h
```

You'll deploy Confluent Platform in a single node configuration.
The representative declarative YAML is here: https://github.com/confluentinc/cfk-workshop/blob/main/dev/confluent-platform-minikube.yaml

```
kubectl apply -f ~/code/cfk-workshop/dev/confluent-platform-minikube.yaml -n confluent

kubectl get pods -w -n confluent
```


## Build the Spring application

WIP

```
cd ~/code/cfk-workshop/apps

skaffold build
```

## Look at production and management cluster

Let's look at Tanzu Mission Control.

Let's look at the prod cluster Control Center.

Let's review the prod cluster declarative YAML

Let's scale up prod cluster

In Tanzu Mission Control, add a dev cluster

## Get data from prod into dev

We have a production deployment with Confluent Platform on VMWare Tanzu.

In this exercise, you'll pull data from a topic in the production cluster in to your development cluster.

To do this, you will:

- define a Cluster Link between the production cluster and the development cluster
- create a mirror topic on the development cluster to mirror all data in the production topic in to the mirror topic on the development cluster

Cluster Linking allows you to directly connect clusters together and mirror topics from one cluster to another. 
Cluster Linking makes it much easier to build multi-datacenter, multi-cluster, and hybrid cloud deployments.

The production cluster has TLS network encryption enabled. In order to connect to it, the development cluster will need to trust the certificate authority used to create the server certificates in production.

Add the certificate authority truststore to be used by the development cluster:

```
kubectl cp ~/code/dev/truststore.p12 kafka-0:/home/appuser/truststore.p12
```

You'll create a cluster link to pull data from the production cluster and in to the development cluster.
In order to do that, you'll need to specify the Kafka properties to connect to the production cluster's Kafka listener.

This configuration includes the following:

- bootstrap.servers: The production cluster's Kafka listener endpoint
- sasl.jaas.config, sasl.mechanism, security.protocol: The SASL Plain configuration and credentials to use to connect to the production cluster
- ssl.truststore.location, ssl.truststore.password: The truststore to use in order to trust the production certificate authority

Exec SSH in to the Kafka broker pod and use the CLI tools to create a cluster link:

```
kubectl exec kafka-0 -it bash

# Create configs

cat << EOF > kafka.properties
bootstrap.servers=kafka.cfk-demo.app:9092
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=devuser password=dev-password;
sasl.mechanism=PLAIN
security.protocol=SASL_SSL
ssl.truststore.location=/home/appuser/truststore.p12
ssl.truststore.password=mystorepassword
EOF

# Create cluster link

kafka-cluster-links --bootstrap-server localhost:9092 --create --link-name datagen-link --config-file kafka.properties

```

Now that a cluster link is created, you will create a mirror topic on the development cluster.
A cluster link connects a mirror topic to its source topic. Any messages produced to the source topic are mirrored over the cluster link to the mirror topic.
Mirror topics are byte-for-byte, offset-preserving asynchronous copies of their source topics. They are read-only; you can consume them the same as any other topic, but you cannot produce into them.

```
# Create a mirror topic

kafka-topics --bootstrap-server localhost:9092 --create --topic prod-quotes --link-name datagen-link --mirror-topic prod-quotes
```

### Review data

Log in to Control Center. You had set up a connection to Control Center in the prior exercise. Use the same URL.

Once logged in, navigate to the topic section. You should see the "prod-quotes" topic


## Set up Argo on dev environment

First, create a namespace where you will install ArgoCD.

kubectl create namespace argocd

Second, apply this install script:

kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

This command will complete quickly, but pods will still be spinning up on the back end. These need to be in a running state before you can move forward. Use the watch command to ensure the pods are running and ready.

watch kubectl get pods -n argocd

Since this is a demo environment, use port-forward to expose a port to the service, and forward it to localhost.

kubectl port-forward --address 0.0.0.0 svc/argocd-server -n argocd 8080:443

In the UI, you will not be able to log in yet. ArgoCD generated a custom password for every deploy. 

The following command will list the pods and format the output to provide just the line you want. It will have the format argocd-server-<number>-<number>.

kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

To log in to the ArgoCD UI, the default username is admin and the default password is the output of the above command.

Log in through the Argo CLI. You will need to accept the server certificate error.

argocd login localhost:8080

Now you'll add the target Kubernetes cluster for ArgoCD to deploy to. This will be the minikube cluster you are on.

Get the Kubernetes contexts:

kubectl config get-contexts -o name
minikube

Add this context as the target

argocd cluster add minikube

## Deploy CFK through ArgoCD

Create a namespace for the CFK through ArgoCD gitops deployment.

kubectl create ns gitops-confluent

Create a new application, using the ArgoCD UI.

Log in with `admin` username and the password you got from this command:

```
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

Click on "New App" in the top left of the UI screen.

Set the application name - for example cfk-minikube.

Select the `default` project.

Keey sync mode as `Manual`.

Set the Github repo URL - `https://github.com/confluentinc/cfk-workshop.git`

Set the path to the `gitops-dev` directory.

Select your target minikube cluster - `https://kubernetes.default.svc`.

Set the target namespace to the one you just created - `gitops-confluent`.

Click "Create".

This will deploy 

## Troubleshoot

### CFK deployment needs to be restarted

Uninstall deployment

Stop minikube

Start minikube

Re-deploy


----

## To Organize


### Create truststore

keytool -import -trustcacerts -noprompt \
  -alias rootCA \
  -file $TUTORIAL_HOME/certs/cacerts.pem \
  -keystore $TUTORIAL_HOME/client/client.truststore.p12 \
  -deststorepass mystorepassword \
  -deststoretype pkcs12

keytool -import -trustcacerts -noprompt \
  -alias rootCA \
  -file /Users/rohitbakhshi/dev/confluent/rohit-cfk-examples/assets/certs/component-certs/generated/ca.pem \
  -keystore /Users/rohitbakhshi/dev/confluent/rohit-cfk-examples/assets/certs/component-certs/generated/client.truststore.p12 \
  -deststorepass mystorepassword \
  -deststoretype pkcs12

/Users/rohitbakhshi/dev/confluent/rohit-cfk-examples/assets/certs/component-certs/generated/ca.pem
/Users/rohitbakhshi/dev/confluent/rohit-cfk-examples/assets/certs/component-certs/generated/ca.pem
# Confluent for Kubernetes Workshop

In this workshop, you will:

- Launch your own Minikube for a local development environment and access an existing Tanzu Kubernetes Grid for production
- Deploy applications and the Confluent for Kubernetes platform for data in motion across both environments
- Easily mirror data from the production environment to the development environment
- Manage infrastructure declaratively and use GitOps to safely update applications
-See how Tanzu Mission Control is used as a global control plane across both development and production Kubernetes environments

## The Lab Environment

Your lab environment is an Ubuntu 18.04 server with all pre-requisites installed.

### How to ssh into the lab machine from local machine

Generate a ssh key on your local machine.

```
ssh-keygen

ls -al <where_keys_created>
  -rw-------   1 me  staff   2.5K Aug 17 13:19 new_key
  -rw-r--r--   1 me  staff   581B Aug 17 13:19 new_key.pub

cat new_key.pub
  ssh-rsa <public key content>
```

On the lab machine, add the public key to authorized_keys file

```
## Add content to a newline in this file
vi ~/.ssh/authorized_keys
```

Now, SSH into the lab machine from your local machine

```
ssh -i new_key ubuntu@<lab-dynamic-dns-name>
```

## Start minikube

Your development environment consists of a functionally complete Confluent Platform deployed to minikube.

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

Cluster links are between two Kafka clusters.
Created on the destination Kafka cluster.

Add certificate authority truststore for use by CFK

```
kubectl cp ~/code/dev/truststore.p12 kafka-0:/home/appuser/truststore.p12
```

Exec in to Kafka pod and create cluster link

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

TODO: Create mirror topic


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
# Confluent for Kubernetes Workshop

In this workshop, you will:

## The Lab Environment

Your lab environment is an Ubuntu 18.04 server with all pre-requisites installed.

### How to add ssh into the lab machine

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


## To Organize

### Get the code

```
cd code

git clone https://github.com/confluentinc/cfk-workshop.git

git clone https://github.com/confluentinc/confluent-kubernetes-examples.git
```

### Deploy CFK onto minikube

Start minikube cluster

```
minikube start

minikube status
  minikube
  type: Control Plane
  host: Running
  kubelet: Running
  apiserver: Running
  kubeconfig: Configured
```

Deploy Confluent for Kubernetes

```
# Create namespace to use
kubectl create ns confluent

# Set namespace for current context to `confluent`
kubectl config set-context --current --namespace confluent

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

Deploy a mini Confluent Platform

```
cd /home/ubuntu/code/confluent-kubernetes-examples/quickstart-deploy  

kubectl apply -f confluent-platform-mini.yaml -n confluent

kubectl get pods -w -n confluent
...
```

### Log in and create cluster links

Cluster links are between two Kafka clusters.
Created on the destination Kafka cluster.

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

cat << EOF > kafka.properties
bootstrap.servers=kafka.cfk-demo.app:9092
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=devuser password=dev-password;
sasl.mechanism=PLAIN
security.protocol=SASL_SSL
ssl.truststore.location=/Users/rohitbakhshi/dev/confluent/cfk-workshop/truststore.p12
ssl.truststore.password=mystorepassword
EOF

kafka-console-producer --bootstrap-server kafka.cfk-demo.app:9092 \
  --topic elastic-0 \
  --producer.config kafka.properties

# Crate cluster link

kafka-cluster-links --bootstrap-server localhost:9092 --create --link-name datagen-link --config-file kafka.properties

```

### move

```
scp -i ~/dev/software/security/rohit-confluent-ps /Users/rohitbakhshi/dev/confluent/cfk-workshop/truststore.p12 ubuntu@ehvmdc9yb9vmxgxlw-lnnmzlpejd6gswtzg.labs.strigo.io:truststore.p12

```

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

### How to add a ssh key to the lab machine

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

### Trust 
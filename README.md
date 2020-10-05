# Redis Secure Client - Homomorphic Encryption

![build and test](https://github.com/aanciaes/redis-homomorphic-enc/workflows/Build%20and%20Test/badge.svg)
[![GitHub release](https://img.shields.io/github/release/aanciaes/secure-redis-proxy.svg)](https://github.com/aanciaes/secure-redis-proxy/releases/)
[![GitHub issues](https://img.shields.io/github/issues/aanciaes/secure-redis-proxy.svg)](https://github.com/aanciaes/secure-redis-proxy/issues/)

This project is an example of a client interacting with a non-secure Redis server.
To maintain the data secure, it encrypts all data before communicating with the Redis server, therefor, all data stored 
inside the server is completely encrypted and not accessible to anyone without the decryption keys, including rouge system 
administrators.

To allow for faster operations, it is implemented a system with partial homomorphic encryption. which allows fot key
discovery in the encrypted redis space. Keys are then encrypted with a deterministic encryption mechanism, so they can be
re-calculated and search in the redis server. Lists with scores use a encryption mechanism that maintains them ordered,
so they can be searched with a range (eg. return all values with scores between 1 and 5), never revealing the actual scores
to the Redis server.

## How to Run

### Api Mode
1. Make sure to have a Redis Server running. To deploy a local Redis Server check out the [Deploy a Local Redis Server](#deploy-a-local-redis-server) bellow.
2. Make sure to have a Keycloak server running (SSO). To deploy a local SSO Server check out the [Deploy a Local Keycloak Server](#deploy-a-local-keycloak-server) bellow.
3. Navigate to `src/main/resources/application.conf` and change the configuration at will.
4. From project root run: `./gradlew clean build`
5. From project root run: `./gradlew bootRun` to run API mode or `./gradlew runCommandLine` to run command line mode.
6. Login to SSO and save the Access Token.
7. Use API with the access token.

### Api Mode with Docker

1. Make sure to have a Redis Server running. To deploy a local Redis Server check out the [Deploy a Local Redis Server](#deploy-a-local-redis-server) bellow.
3. Make sure to have a Keycloak server running (SSO). To deploy a local SSO Server check out the [Deploy a Local Keycloak Server](#deploy-a-local-keycloak-server) bellow.
4. Navigate to `src/main/resources/application.conf` and change the configuration at will.
5. From project root run: `docker build -t secure-redis-proxy:dev .`
6. From project root run: `docker run -i -t -p 8443:8443 secure-redis-proxy:dev`
6. Login to SSO and save the Access Token.
7. Use API with the access token.

## Run in Production

To build the production image:

1. `docker build -f prod.Dockerfile -t secure-redis-proxy:prod .`
2. To test on local machine run: `docker run --rm --name secure-redis-proxy -it -p 8777:8777 secure-redis-proxy:prod`

### Push to docker hub

1. Login to docker hub
2. Build image with version as tag:
3. `docker build -f prod.Dockerfile -t aanciaes/secure-redis-proxy:0.1.1 .`
4. `docker push aanciaes/secure-redis-proxy:0.1.1`

If image is ready for production, build the prod tag and push:

1. `docker build -f prod.Dockerfile -t aanciaes/secure-redis-proxy:latest .`
2. `docker push aanciaes/secure-redis-proxy:latest`

### Running on Production Environment

1. Login to docker
2. `docker run --rm --name secure-redis-proxy -it -d -p 8777:8777 --device=/dev/isgx -e SCONE_MODE=HW aanciaes/secure-redis-proxy:latest`

**Notes:**

1. The deployment on the cloud provider should be done by uploading the production image to the docker hub registry and pull from there to avoid any losses.
2. Be aware of the redis ports with the docker run command as they may change in the `spring-application.yml` configuration file.

### Deploy Authentication Server to Production

1. Login to docker
2. `docker run --rm --name secure-redis-sso -it -d -p 8678:8443 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=mq@cbGfvM@bT3P_Q.zFcACfMT aanciaes/thesis-auth-server:prod`

## Application Configurations

The available application configuration are explained below:

```
application.secure=true/false

# Redis Server Configurations
redis.host=localhost
redis.port=6379

# Redis Authentication Configurations
redis.auth=true/false
redis.auth.username=default
redis.auth.password=redis

# Redis TLS Configurations
redis.tls=true/false
redis.tls.keystore.path=/path/to/keystore
redis.tls.keystore.password=keystore-password
redis.tls.truststore.path=/path/to/truststore
redis.tls.truststore.password=truststore-password

# Redis Replication Configurations
redis.replication=true/false
redis.replication.mode=MasterSlave/Cluster
redis.replication.nodes=localhost:7001|8542, localhost:7002|8543

# Homomorphic Encryption Configurations
key.encryption.det.secret=rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAABD/0YUynK927L2L+Hs1YCGk
key.encryption.ope.secret=rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAABD/0YUynK927L2L+Hs1YCGk

# Data Encryption Configurations
data.encryption.algorithm=AES/ECB/PKCS5Padding
data.encryption.provider=SunJCE
data.encryption.keystore.type=jks
data.encryption.keystore.path=keystores/keystore.jks
data.encryption.keystore.password=secretpassword
data.encryption.keystore.keyName=encryptionkey
data.encryption.keystore.keyPassword=secretpassword

# Data Signature Configurations
data.signature.algorithm=SHA512withRSA
data.signature.provider=SunRsaSign
data.signature.keystore.type=jks
data.signature.keystore.path=keystores/keystore.jks
data.signature.keystore.password=secretpassword
data.signature.keystore.keyName=signaturekey
data.signature.keystore.keyPassword=supersecretpassword

# Data Integrity Configurations
data.hmac.algorithm=HMacSHA256
data.hmac.provider=SunJCE
data.hmac.keystore.type=PKCS12
data.hmac.keystore.path=keystores/keystore.p12
data.hmac.keystore.password=secretpassword
data.hmac.keystore.keyName=integritykey
data.hmac.keystore.keyPassword=secretpassword
```

`application.secure` - true if data stored in redis is completly encrypted using homomorphic ciphers.

`redis.host` - Redis Host

`redis.port` - Redis Port

`redis.auth` - True if redis authentication is enabled.

`redis.auth.username` - Redis username. Leave blank or commented if no username exists.

`redis.auth.password` - Redis password.

`redis.tls` - true if Redis TLS communication is enabled.

`redis.tls.keystore.path` - Path to keystore holding redis-cli key pair.

`redis.tls.keystore.password` - Password of the keystore.

`redis.tls.truststore.path` - Path to keystore holding redis-server certificate.

`redis.tls.truststore.password` - Password of the keystore.

`redis.replication` - True if Redis is running in any replication mode.

`redis.replication.mode` - Defines the replication mode of the Redis. Options are MasterSlave or Cluster.

`redis.replication.nodes` - List of node contact points of Redis Replication. All nodes of any replication node should be listed so
proxy can attest all nodes of the infrastructure. List separated by commas in form of `host:port|attestationPort`.

`key.encryption.det.secret` - Base64 secret key to encrypt the Redis keys in deterministic form.

`key.encryption.ope.secret` - Base64 secret key to encrypt the Redis scores in an ordered form.

`data.encryption.algorithm` - Encryption cipher suite to encrypt Redis Values.

`data.encryption.provider` - Provider of the encryption cipher suite.

`data.encryption.keystore.type` - Keystore type where encryption AES key is stored.

`data.encryption.keystore.path` - Keystore path where encryption AES key is stored.

`data.encryption.keystore.password` - Keystore password where encryption AES key is stored.

`data.encryption.keystore.keyName` - Name of the key to encrypt data.

`data.encryption.keystore.keyPassword` - Password of the key to encrypt data.

`data.signature.algorithm` - Signature algorithm to sign data.

`data.signature.provider` - Signature provider for data signature.

`data.signature.keystore.type` - Keystore type where signature RSA key pair is stored.

`data.signature.keystore.path` - Keystore path where signature RSA key pair is stored.

`data.signature.keystore.password` - Keystore password where signature RSA key pair is stored.

`data.signature.keystore.keyName` - Name of the key pair to sign data.

`data.signature.keystore.keyPassword` - Password of the key pair to sign data.

`data.hmac.algorithm` - HMac algorithm to verify data integrity.

`data.hmac.provider` - Hmac provider for data integrity verification.

`data.hmac.keystore.type` - Keystore type where Hmac sym key is stored.

`data.hmac.keystore.path` - Keystore where Hmac sym key is stored.

`data.hmac.keystore.password` - Keystore password where Hmac sym key is stored.

`data.hmac.keystore.keyName` - Name of the key for hmac data integrity verification.

`data.hmac.keystore.keyPassword` - Password of the key for hmac data integrity verification.

## Deploy a Local Redis Server

To quickly deploy a Redis server using Docker use `docker run -d -p 6379:6379 redis:6.0.3`.

To deploy a Redis server with a custom configuration file, modify the config file provided [here](redis.conf) and run
from the project root the command `docker run -d -p 6379:6379 -v $PWD/redis.conf:/usr/local/etc/redis/redis.conf redis:6.0.3 redis-server /usr/local/etc/redis/redis.conf`

## Deploy a Local Keycloak Server

To quickly deploy a Keycloak server using Docker use the [sso-deploy.sh](sso-deploy.sh), `./sso-deploy.sh`.
It deploys a keycloak server with an administrator user `miguel:miguel` and a basic user `joao:joao`.

## Login and Retrieve Access Token from SSO

To login, use the request below. Change the server name and the user and password.
```
curl -X POST \
  https://sso-example.com/auth/realms/thesis-realm/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&username=user&password=pass&client_id=thesis-redis-client'
```

An access token will be returned, that should be stored and used on the authorization header for request to the proxy API.

## Enhancements and Bugs

Check github issues [here](https://github.com/aanciaes/redis-homomorphic-enc/issues).

## Milestones

Check github milestones [here](https://github.com/aanciaes/redis-homomorphic-enc/milestones).

Current milestones:

* [0.4](https://github.com/aanciaes/redis-homomorphic-enc/milestone/5)

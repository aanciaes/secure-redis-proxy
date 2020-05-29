# Redis Secure Client - Homomorphic Encryption

![Build and Test](https://github.com/aanciaes/redis-homomorphic-enc/workflows/Build%20and%20Test/badge.svg)

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

1. Make sure to have a Redis Server running. To deploy a local Redis Server check out the [Deploy a Local Redis Server](#deploy-a-local-redis-server) bellow.
2. Navigate to `src/main/resources/application.conf` and change the configuration at will.
3. From project root run: `./gradlew clean build`
4. From project root run: `./gradlew run`

### Available Operations

```
Usage: $ command [options]

List of available commands:
    set <key> <value> [expiration [ms|s]]
    get <key>
    del <key>
    zadd <key> <score> <value>
    zrange <key> <min> <max>
    flushall
    help
    exit
```

### Application Configurations

The available application configuration are explained below:

```
application.secure=true

# Redis Server Configurations
redis.host=localhost
redis.port=6379

# Redis Authentication Configurations
redis.auth.username=
redis.auth.password=

# Redis TLS Configurations
redis.tls=false
redis.tls.keystore.path=/path/to/keystore
redis.tls.keystore.password=keystore-password
redis.tls.truststore.path=/path/to/truststore
redis.tls.truststore.password=truststore-password

# Redis Cluster Configurations
redis.cluster=false
redis.cluster.nodes=localhost:7001

# Homomorphic Encryption Configurations
key.encryption.det.secret=rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAABD/0YUynK927L2L+Hs1YCGk
key.encryption.ope.secret=rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAABD/0YUynK927L2L+Hs1YCGk

# Data Encryption Configurations
data.encryption.algorithm=AES/ECB/PKCS5Padding
data.encryption.provider=SunJCE
data.encryption.secret=emrWoixAHOm0nDOEMvnTmcrKRhQvIIyT

# Data Signature Configurations
data.signature.algorithm=SHA512withRSA
data.signature.provider=SunRsaSign
data.signature.keystore.type=jks
data.signature.keystore.path=keystores/keystore.jks
data.signature.keystore.password=secretpassword
data.signature.keystore.keyName=signaturekey
data.signature.keystore.keyPassword=supersecretpassword
```

`application.secure` - true if data stored in redis is completly encrypted using homomorphic ciphers.

`redis.host` - Redis Host

`redis.port` - Redis Port

`redis.auth.username` - Redis username. Leave blank or commented if no username exists or auth is disabled.

`redis.auth.password` - Redis password. Leave blank if auth is disabled.

`redis.tls` - true if Redis TLS communication is enabled.

`redis.tls.keystore.path` - Path to keystore holding redis-cli key pair.

`redis.tls.keystore.password` - Password of the keystore.

`redis.tls.truststore.path` - Path to keystore holding redis-server certificate.

`redis.tls.truststore.password` - Password of the keystore.

`redis.cluster` - True if Redis is running in cluster mode.

`redis.cluster.nodes` - List of node contact points of Redis Cluster. Redis will automatically try to find all cluster nodes from one single contact point.
List separated by commas in form of `host:port`.

`key.encryption.det.secret` - Base64 secret key to encrypt the Redis keys in deterministic form.

`key.encryption.ope.secret` - Base64 secret key to encrypt the Redis scores in a ordered form.

`data.encryption.algorithm` - Encryption cipher suite to encrypt Redis Values.

`data.encryption.provider` - Provider of the encryption cipher suite.

`data.encryption.secret` - Secret of the encryption cipher suite.

`data.signature.algorithm` - Signature algorithm to sign data.

`data.signature.provider` - Signature provider for data signature.

`data.signature.keystore` - Keystore where signature RSA key pair is stored.

`data.signature.keystore.type` - Keystore type where signature RSA key pair is stored.

`data.signature.keystore.path` - Keystore path where signature RSA key pair is stored.

`data.signature.keystore.password` - Keystore password where signature RSA key pair is stored.

`data.signature.keystore.keyName` - Name of the key pair to sign data.

`data.signature.keystore.keyPassword` - Password of the key pair to sign data.

## Deploy a Local Redis Server

To quickly deploy a Redis server using Docker use `docker run -d -p 6379:6379 redis:6.0.3`.

To deploy a Redis server with a custom configuration file, modify the config file provided [here](redis.conf) and run
from the project root the command `docker run -d -p 6379:6379 -v $PWD/redis.conf:/usr/local/etc/redis/redis.conf redis:6.0.3 redis-server /usr/local/etc/redis/redis.conf`

## Enhancements and Bugs

Check github issues [here](https://github.com/aanciaes/redis-homomorphic-enc/issues).

## Milestones

Check github milestones [here](https://github.com/aanciaes/redis-homomorphic-enc/milestones).

Current milestones:

* [0.1](https://github.com/aanciaes/redis-homomorphic-enc/milestone/1)
* [0.2](https://github.com/aanciaes/redis-homomorphic-enc/milestone/2)
* [0.3](https://github.com/aanciaes/redis-homomorphic-enc/milestone/3)

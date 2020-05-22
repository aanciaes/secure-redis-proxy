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

1. A Redis server must be running.
To quickly deploy a Redis server using Docker use `docker run -d -p 6379:6379 redis:6.0.3`.

2. Navigate to `src/main/resources/application.conf` and change the configuration at will.
3. `./gradlew clean build`
4. `./gradlew run`

### Available Operations

```
Usage: $ command [options]

List of available commands:
    set <key> <value>
    get <key>
    del <key>
    zadd <key> <score> <value>
    zrange <key> <min> <max>
    flushall
    help
    exit
```

## Enhancements and Bugs

Check github issues [here](https://github.com/aanciaes/redis-homomorphic-enc/issues).

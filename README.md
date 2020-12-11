# Ransomware-Resistant Remote Documents

## Prerequisites

+ Linux based OS (64 bit)
+ Java 14
+ MySQL database or anything compatible (e.g.: MariaDB)
+ Zookeeper

## Building the solution

Beforing compiling the project, start the MySQL server and create a new database.
Configure and start the Zookeeper server.

### Install ZKNaming library

```bash
$ cd SIRS-Project/naming/
$ mvn install -DskipTests
```

### Configure server properties

```bash
$ cd SIRS-Project/server/src/main/resources/
```
In this folder there is a file named "application.properties.example". Fill the info specifying the database name, username and password. Rename the file to "application.properties".

### Compile and install all source files

```bash
$ cd SIRS-Project/
$ mvn install
```

## Running the solution

### Server

```bash
$ cd SIRS-Project/server/
$ mvn spring-boot:run -Dspring-boot.run.arguments="NAME_SERVER_HOST NAME_SERVER_PORT SERVER_HOST SERVER_PORT PRIMARY"
```

`NAME_SERVER_HOST`: host name of the zookeeper name server.

`NAME_SERVER_PORT`: port of the zookeeper name server, usually 2181.

`SERVER_HOST`: server hostname.

`SERVER_PORT`: server port.

`PRIMARY` can take the following values:
+ 1: The server launched will be the primary server
+ 0: The server launched will be the backup server

The primary server must be launched first.

**IMPORTANT**: If you are running a primary and backup server they will need to run on different databases, or some operations will not work. For testing it is better to just run a primary server.

### Client

```bash
$ cd SIRS-Project/client/
$ mvn exec:java -Dexec.args="NAME_SERVER_HOST NAME_SERVER_PORT"
```

**IMPORTANT**: if the server is reset at anytime all keystores and the file cache must be removed in the client: `rm *.ks && rm .fileCache.json`.

## Usage

The client supports the following commands:

+ `register USERNAME`: registers the user in the server and asks to set their password.
+ `login USERNAME`: asks for the user's password and if valid authenticates the user in the server for the remainder of the session or until someone else logs in.
+ `upload FILENAME`: uploads the file to the server. File must be located in the current directory.
+ `download FILENAME`: downloads the file from the server if the current user has access to it.
+ `unlock FILENAME`: decrypts local version of file without contacting the server.all
+ `invite USERNAME FILENAME`: invites user to edit the file.
+ `accept FILENAME`: accepts an invitation to edit the file.
+ `remove USERNAME FILENAME`: removes a user's ability to edit the file.

## Example

![Screenshot](https://media.discordapp.net/attachments/360793474624389130/787047932989800458/unknown.png?width=481&height=702)
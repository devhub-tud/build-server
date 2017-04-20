DevHub Build-server
======
DevHub is a software system designed to give students a simple practical introduction into modern software development. It provides an environment in which they can work on practical assignments without setting up their own (private) code repository or a Continuous Integration server. The environment is also designed to give students a working simple workflow largely based on GitHub's pull requests system. 

This repository contains the build server used for Devhub. The build server has a REST API and builds any git repositories safely in isolated Docker containers. The build result is then returned through a web hook.

Build a development system
------------

On deployed systems running under Linux the docker host can be on the same system as the build server instance. For development under Windows and OS X, a docker host VM is required (for example [Boot2Docker](http://boot2docker.io)). The build server instance clones the repositories to a working directory shared with the Docker container, for this to work in a VM, we need to make a shared folder between the host computer and the Docker host VM.

```sh
brew install docker
brew install boot2docker
sudo mkdir /workspace
boot2docker init
VBoxManage sharedfolder add "boot2docker-vm" --name "workspace" --hostpath "/workspace"
boot2docker up
```

Then ssh into the boot2docker virtual machine to mount the shared folder. 
```sh
boot2docker ssh
sudo mkdir /workspace
sudo mount -t vboxsf -o uid=1000,gid=50 workspace /workspace
```

### Notes for docker for mac ###
Because we are using a older version of the spotify docker client.
The way we instiate the client doens't work with docker for mac out of the box.

The following instructions from the spotify client readme should fix it:

A note on using Docker for Mac

If you are using Docker for Mac and DefaultDockerClient.fromEnv(), it might not be clear what value to use for the DOCKER_HOST environment variable. The value you should use is DOCKER_HOST=unix:///var/run/docker.sock, at least as of version 1.11.1-beta11.

Add docker images to Docker
------------
On the Docker host, navigate to a folder containing a `Dockerfile` and run the following command. (This example is for the 'java-maven' Dockerfile in the repository)

```sh
build -t java-maven .
```


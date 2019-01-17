#!/bin/bash

ETCD_VERSION=3.3.11
ETCD_RELEASE=etcd-v${ETCD_VERSION}-linux-amd64

# DOWNLOAD ETCD
wget --quiet https://github.com/etcd-io/etcd/releases/download/v${ETCD_VERSION}/${ETCD_RELEASE}.tar.gz

# INSTALL BINARIES
tar xzf ${ETCD_RELEASE}.tar.gz
sudo cp ${ETCD_RELEASE}/etcd /usr/bin/
sudo cp ${ETCD_RELEASE}/etcdctl /usr/bin/

# CREATE USER FOR ETCD
sudo adduser --disabled-password --gecos "" etcd

# CREATE ETCD DATA FOLDER
sudo mkdir /var/lib/etcd
sudo chown etcd /var/lib/etcd

# CREATE SYSTEM SERVICE
sudo cp etcd.service /etc/systemd/system/

# START SERVICE
sudo systemctl start etcd

# ENABLE SERVICE STARTUP ON BOOT
sudo systemctl enable etcd.service

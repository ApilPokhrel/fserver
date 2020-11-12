#!/bin/sh
sudo kill -9 `sudo lsof -t -i:3609`
nohup mvn spring-boot:run &

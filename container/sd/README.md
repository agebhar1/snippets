```sh
$ curl --silent --no-buffer --unix-socket /var/run/docker.sock http://-/events | jq
{
  "status": "exec_create: kill -SIGUSR1 1 ",
  "id": "b1345d99815e09581672b9549bb7189444451c3b0a2a02ff7f7f9ed84ac8fa4d",
  "from": "docker.io/nginx:1.25.5-alpine3.19",
  "Type": "container",
  "Action": "exec_create: kill -SIGUSR1 1 ",
  "Actor": {
    "ID": "b1345d99815e09581672b9549bb7189444451c3b0a2a02ff7f7f9ed84ac8fa4d",
    "Attributes": {
      "com.docker.compose.config-hash": "be343d92bc63fdcc7685c6a5740549f9f55f874fc81df0fff1d69d3e3dc08b56",
      "com.docker.compose.container-number": "1",
      "com.docker.compose.depends_on": "srv:service_started:false",
      "com.docker.compose.image": "sha256:501d84f5d06487ff81e506134dc922ed4fd2080d5521eb5b6ee4054fa17d15c4",
      "com.docker.compose.oneoff": "False",
      "com.docker.compose.project": "sd",
      "com.docker.compose.project.config_files": "/home/vagrant/src/github.com/agebhar1/snippets/container/sd/compose.yml",
      "com.docker.compose.project.working_dir": "/home/vagrant/src/github.com/agebhar1/snippets/container/sd",
      "com.docker.compose.service": "nginx",
      "com.docker.compose.version": "2.17.3",
      "execID": "0a80c6af28b8af28fa660b0a78e35bb97984d45f9b0ac7eed73ef962cab29771",
      "image": "docker.io/nginx:1.25.5-alpine3.19",
      "maintainer": "NGINX Docker Maintainers <docker-maint@nginx.com>",
      "name": "sd-nginx-1"
    }
  },
  "scope": "local",
  "time": 1716193837,
  "timeNano": 1716193837175522800
}
```
```sh
$ curl --unix-socket /var/run/docker.sock --header "Content-Type: application/json" --data @req.json http:/-/containers/{id}/exec
```



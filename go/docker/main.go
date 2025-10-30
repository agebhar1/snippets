package main

import (
	"context"
	"fmt"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/client"
)

func main() {

	args := filters.NewArgs()
	args.Add("label", "com.docker.compose.service=keycloak")
	args.Add("label", "com.docker.compose.service=nginx")

	opts := []client.Opt{
		client.WithHostFromEnv(),
		client.WithAPIVersionNegotiation(),
	}

	c, err := client.NewClientWithOpts(opts...)
	if err != nil {
		panic(err)
	}

	ctx := context.Background()

	containers, err := c.ContainerList(ctx, container.ListOptions{
		Filters: args,
	})
	if err != nil {
		panic(err)
	}

	for _, c := range containers {
		fmt.Printf("ID: %s, Names: %v\n", c.ID, c.Names)
	}

	_ = c.Close()
}

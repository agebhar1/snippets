/*
Copyright 2016 The Kubernetes Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

// Note: the example only works with the code within the same release/branch.
package main

import (
	"context"
	"fmt"
	"io"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	//
	// Uncomment to load all auth plugins
	// _ "k8s.io/client-go/plugin/pkg/client/auth"
	//
	// Or uncomment to load specific auth plugins
	// _ "k8s.io/client-go/plugin/pkg/client/auth/oidc"
)

func sayHello(pod *corev1.Pod) {
	if pod.Status.Phase == corev1.PodRunning {
		res, err := http.Get("http://" + pod.Status.PodIP + ":8080")
		if err != nil {
			return
		}

		defer func() { _ = res.Body.Close() }()

		body, err := io.ReadAll(res.Body)
		if err != nil {
			return
		}
		fmt.Printf("GET %s:8080 => %s\n", pod.Status.PodIP, string(body))
	}
}

func run() {

	srv := http.Server{
		Addr: ":8080",
	}
	srv.Handler = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Printf("Got request from %s :)\n", r.RemoteAddr)

		w.WriteHeader(http.StatusOK)
		w.Header().Set("Content-Type", "text/plain")
		_, _ = fmt.Fprintln(w, "Hello World")
	})
	go func() {
		_ = srv.ListenAndServe()
	}()

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	for _, e := range os.Environ() {
		fmt.Printf("%s\n", e)
	}

	// creates the in-cluster config
	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}
	// creates the clientset
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

	// get pods in all the namespaces by omitting namespace
	// Or specify namespace to get pods in particular namespace
	pods, err := clientset.CoreV1().Pods("default").List(context.TODO(), metav1.ListOptions{
		LabelSelector: "app=demo",
	})
	if err != nil {
		panic(err.Error())
	}

	var latestResourceVersion uint64 = 0

	fmt.Printf("There are %d pods in the cluster:\n", len(pods.Items))
	for _, pod := range pods.Items {
		fmt.Printf("\tPod: %s %s %s %s\n", pod.Name, pod.ResourceVersion, pod.Status.Phase, pod.Status.PodIP)
		sayHello(&pod)

		resourceVersion, err := strconv.ParseUint(pod.ResourceVersion, 10, 64)
		if err == nil && latestResourceVersion < resourceVersion {
			latestResourceVersion = resourceVersion
		}
	}

	watcher, err := clientset.CoreV1().Pods("default").Watch(context.TODO(), metav1.ListOptions{
		LabelSelector: "app=demo",
	})
	if err != nil {
		panic(err.Error())
	}

	go func() {
		_ = <-sigs
		_ = srv.Shutdown(context.Background())
		watcher.Stop()
	}()

	for event := range watcher.ResultChan() {
		pod, ok := event.Object.(*corev1.Pod)
		if ok {
			resourceVersion, err := strconv.ParseUint(pod.ResourceVersion, 10, 64)
			if err == nil && latestResourceVersion < resourceVersion {
				fmt.Printf("Pod Event Received: %s %s name: %s Phase: %s PodIP: %s\n", pod.ResourceVersion, event.Type, pod.Name, pod.Status.Phase, pod.Status.PodIP)
				if event.Type == watch.Modified {
					sayHello(pod)
				}
			}
		} else {
			fmt.Printf("Pod Event Received: %s %v\n", event.Type, event.Object)
		}
	}

	// Examples for error handling:
	// - Use helper functions e.g. errors.IsNotFound()
	// - And/or cast to StatusError and use its properties like e.g. ErrStatus.Message
	//_, err = clientset.CoreV1().Pods("default").Get(context.TODO(), "example-xxxxx", metav1.GetOptions{})
	//if errors.IsNotFound(err) {
	//	fmt.Printf("Pod example-xxxxx not found in default namespace\n")
	//} else if statusError, isStatus := err.(*errors.StatusError); isStatus {
	//	fmt.Printf("Error getting pod %v\n", statusError.ErrStatus.Message)
	//} else if err != nil {
	//	panic(err.Error())
	//} else {
	//	fmt.Printf("Found example-xxxxx pod in default namespace\n")
	//}
}

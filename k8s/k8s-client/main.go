package main

import (
	"context"
	"fmt"
	"golang.org/x/sync/errgroup"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/cache"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {

	g, ctx := errgroup.WithContext(context.Background())
	ctx, cancel := context.WithCancel(ctx)

	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}

	client, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

	factory := informers.NewSharedInformerFactoryWithOptions(client, 30*time.Second, informers.WithNamespace("default"), informers.WithTweakListOptions(func(options *metav1.ListOptions) {
		options.LabelSelector = "app=demo"
	}))

	informer := factory.Core().V1().Pods().Informer()

	err = informer.SetWatchErrorHandler(func(r *cache.Reflector, err error) {
		fmt.Printf("error watching pods: %v\n", err)
		cancel()
	})
	if err != nil {
		panic(err.Error())
	}

	handler, err := informer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			pod := obj.(*v1.Pod)
			fmt.Printf("%s - pod added  : %s (%s)\n", pod.GetCreationTimestamp().Format(time.RFC3339), pod.Name, pod.Status.PodIP)
		},
		UpdateFunc: func(old, new interface{}) {
			oldPod := old.(*v1.Pod)
			podNew := new.(*v1.Pod)
			fmt.Printf("%s - pod updated: %s (%s -> %s) (%s)\n", podNew.GetCreationTimestamp().Format(time.RFC3339), podNew.Name, oldPod.ResourceVersion, podNew.ResourceVersion, podNew.Status.PodIP)
		},
		DeleteFunc: func(obj interface{}) {
			pod := obj.(*v1.Pod)
			fmt.Printf("%s - pod deleted: %s (%s)\n", pod.GetCreationTimestamp().Format(time.RFC3339), pod.Name, pod.Status.PodIP)
		},
	})
	if err != nil {
		panic(err.Error())
	}

	g.Go(func() error {
		informer.Run(ctx.Done())
		return nil
	})

	fmt.Println("wait for cache to sync")
	if !cache.WaitForCacheSync(ctx.Done(), handler.HasSynced) {
		panic("failed to wait for cache sync")
	}
	fmt.Println("done: wait for cache to sync")

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	g.Go(func() error {
		<-sigs
		cancel()
		return nil
	})

	_ = g.Wait()
}

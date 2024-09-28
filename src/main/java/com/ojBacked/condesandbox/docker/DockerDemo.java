package com.ojBacked.condesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
//        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();

        /**
         * 拉取镜像，docker有问题，无法拉取
         */
        String image = "hello-world";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像：" + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd
//                .exec(pullImageResultCallback)
//                .awaitCompletion();
//        System.out.println("下载完成");

        /**
         * 创建容器
         */
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd
                .withCmd("echo","hello docker")
                .withName("hello")
                .exec();
        System.out.println(createContainerResponse);
        /**
         * 查看容器状态
         */
//        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
//        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
//        for (Container container : containerList) {
//            System.out.println(container);
//        }

        /**
         * 启动容器
         */
        String containerId = createContainerResponse.getId();
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        System.out.println(startContainerCmd);

    }
}

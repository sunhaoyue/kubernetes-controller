package com.kedacom.ctsp.iomp.k8s;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author hesen
 * @since 2021-09-2021/9/27
 */
@DisplayName("k8s-mock-demo")
@EnableKubernetesMockClient(crud = true)
public class K8sMockServerTest {

    KubernetesClient client;

    @Test
    public void test() {

        Assertions.assertNotNull(client);
    }

    @DisplayName("k8s-mock")
    @Test
    public void testInCrudMode() {

        client.pods().inNamespace("ns1").create(new PodBuilder().withNewMetadata().withName("pod1").endMetadata().build());

        // read
        PodList ns1 = client.pods().inNamespace("ns1").list();
        Assertions.assertNotNull(ns1);
        Assertions.assertEquals(1, ns1.getItems().size());
    }
}

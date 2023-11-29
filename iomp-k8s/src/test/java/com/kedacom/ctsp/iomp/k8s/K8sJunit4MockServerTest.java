package com.kedacom.ctsp.iomp.k8s;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.*;

/**
 * @author hesen
 * @since 2021-09-2021/9/27
 */
public class K8sJunit4MockServerTest {

    @ClassRule
    public static KubernetesServer server = new KubernetesServer(true, true);

    static KubernetesClient client;

    @BeforeClass
    public static void initK8s() {
        client = server.getClient();
    }

    @Test
    public void testInCrudMode() {
        //CREATE
        client.pods().inNamespace("ns1").create(new PodBuilder().withNewMetadata().withName("pod1").endMetadata().build());

        //READ
        PodList podList = client.pods().inNamespace("ns1").list();
        Assert.assertNotNull(podList);
        Assert.assertEquals(1, podList.getItems().size());

    }

}

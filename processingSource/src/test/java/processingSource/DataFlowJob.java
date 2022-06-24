package processingSource;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import static org.hamcrest.CoreMatchers.is;

/**
 * Unit tests for services' health.
 */
public class DataFlowJob 
{

    @Test
    /**
     * Tests whether the kafka connect service is up and running. 
     * @throws IOException
     */
    public void kafkaConnectShouldAnswer() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            // TODO change host to use service name so this works non locally too.
            HttpGet request = new HttpGet("http://localhost:8083/connectors");

            CloseableHttpResponse response = httpClient.execute(request);
            assertTrue(response.getStatusLine().getStatusCode() == 200);

            try {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                assertThat(result,  is("[]"));
                
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }
}

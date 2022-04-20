package com.amazonaws.services.iot.client.util;

import com.amazonaws.services.iot.client.AWSIotException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotWebSocketUrlSignerTest {

    private final String PrefixedEndPoint = "hostname.iot.us-east-1.AMAZONAWS.COM:443";
    private final String EndPoint = "iot.us-east-1.AMAZONAWS.COM";
    private final String AccessKeyId = "123";
    private final String SecretAccessKey = "456";
    private final String SessionToken = "abc";
    private final Date SigningDate = new Date(1451606400000l);
    private final String PrefixedEndPointSigned = "wss://hostname.iot.us-east-1.amazonaws.com:443/mqtt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=123%2F20160101%2Fus-east-1%2Fiotdata%2Faws4_request&X-Amz-Date=20160101T000000Z&X-Amz-SignedHeaders=host&X-Amz-Signature=08b80c7273fa6c9d8bd815081c80e60818f2bf974213993a1cd4463a5785b1ce";
    private final String EndPointSigned = "wss://iot.us-east-1.amazonaws.com/mqtt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=123%2F20160101%2Fus-east-1%2Fiotdata%2Faws4_request&X-Amz-Date=20160101T000000Z&X-Amz-SignedHeaders=host&X-Amz-Signature=b50019dd3ec2d316a6a7938f363f6c792b5f58c04e1e6535d6ae277bf8cf3304";

    @Test
    public void testSigningPrefixEndpoint() throws AWSIotException {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(PrefixedEndPoint, AccessKeyId,
                SecretAccessKey, null);

        String url = urlSigner.getSignedUrl(SigningDate);

        assertEquals(PrefixedEndPointSigned, url);
    }

    @Test
    public void testSigningRegularEndpoint() throws AWSIotException {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, AccessKeyId, SecretAccessKey, null);

        String url = urlSigner.getSignedUrl(SigningDate);

        assertEquals(EndPointSigned, url);
    }

    @Test
    public void testSigningWithSessionToken() throws AWSIotException {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, AccessKeyId, SecretAccessKey,
                SessionToken);

        String url = urlSigner.getSignedUrl(SigningDate);

        assertEquals(EndPointSigned + "&X-Amz-Security-Token=" + SessionToken, url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSigningWithInvalidEndpoint() throws AWSIotException {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner("hostname.iot.us-east-1.AMAZON.COM",
                AccessKeyId, SecretAccessKey, SessionToken);
        urlSigner.getSignedUrl(SigningDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSigningWithCredentials() throws AWSIotException {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, null, null, SessionToken);
        urlSigner.getSignedUrl(SigningDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRegionWithCreds() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, null, null, SessionToken, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRegion() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, null);
    }

     public void testRegionDerivedFromEndpointWithCreds() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, null, null, SessionToken);
        assertEquals("us-east-1", urlSigner.getRegion());
    }

    public void testRegionDerivedFromEndpoint() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint);
        assertEquals("us-east-1", urlSigner.getRegion());
    }

    public void testRegionSetFromTheConstructorWithCreds() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, null, null, SessionToken, "us" +
                "-east-2");
        assertEquals("us-east-2", urlSigner.getRegion());
    }

    public void testRegionSetFromTheConstructor() {
        AwsIotWebSocketUrlSigner urlSigner = new AwsIotWebSocketUrlSigner(EndPoint, "us-east-2");
        assertEquals("us-east-2", urlSigner.getRegion());
    }
}

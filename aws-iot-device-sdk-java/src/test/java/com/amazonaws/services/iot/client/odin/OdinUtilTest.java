package com.amazonaws.services.iot.client.odin;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.core.AwsIotConnectionType;

import amazon.odin.httpquery.OdinMaterialRetriever;
import amazon.odin.httpquery.model.Material;
import amazon.odin.httpquery.model.MaterialPair;
import amazon.odin.httpquery.model.MaterialType;

@RunWith(MockitoJUnitRunner.class)
public class OdinUtilTest {

    private static final String TEST_ENDPOINT = "iot.us-east-1.amazonaws.com";
    private static final String TEST_CLIENTID = "client";
    private static final String ACCESS_KEY_ID = "123";
    private static final String SECRET_ACCESS_KEY = "456";
    private static final String TEST_MATERIAL_SET = "material.set";
    private static final String TEST_CERT =
            "MIIDlTCCAn2gAwIBAgIVAKuR4L6GajQRv1DzXlUFigMoiwzsMA0GCSqGSIb3DQEB" +
            "CwUAME0xSzBJBgNVBAsMQkFtYXpvbiBXZWIgU2VydmljZXMgTz1BbWF6b24uY29t" +
            "IEluYy4gTD1TZWF0dGxlIFNUPVdhc2hpbmd0b24gQz1VUzAeFw0xNTA5MTUwMDEz" +
            "MjhaFw00OTEyMzEyMzU5NTlaMFkxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJXQTEQ" +
            "MA4GA1UEBxMHU2VhdHRsZTEPMA0GA1UEChMGQW1hem9uMQwwCgYDVQQLEwNBV1Mx" +
            "DDAKBgNVBAMTA1NESzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALrx" +
            "n1ZGjBDdasdmuJh8F/KxhMSB/u7f8olGaoPtkvFHkzCf3sXqoHVxzYITzWH8UlmM" +
            "hNJ0CaRfcT/Dmi0PDxPrPQLR1/MjV9WpHTLfI2kA+PI+d4LnnlYQYnQc9wgZIX2c" +
            "+D0mA12By8BRduwM3rDAULmwjjfFX/MLLkDDng+mEIMjXOZnWjMJ3dorSzajVP2C" +
            "bWt1JMRGoSjY2fQVBc2JzU+7y9s9fxMO5329Hne1E8bNVZd+rHJKlJhvIWJCAoya" +
            "NnF4whXMp+UHGPJdhHQCnSPbX5r2c2UdDL/1bftNlX6grCmivuIv6qw+dtA4V7pc" +
            "lsMSEt9zFrVJ6VkZXbUCAwEAAaNgMF4wHwYDVR0jBBgwFoAULXQSP9o80neirjAW" +
            "SlF+yZWjLh0wHQYDVR0OBBYEFCDoMMxiSPyy4D6a5qhg+6FXZtMtMAwGA1UdEwEB" +
            "/wQCMAAwDgYDVR0PAQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IBAQAtlG5ytjMN" +
            "c95dlafQVhPoAKEJ0JkDYl3ZmbNYHXgTQfG08a8zFQLLECODiiO/5HyNaAI3Pzc3" +
            "M580RijF/D23XUHLCvVxaeZgQnJbs+xmHPIeFxCiGfBXBTET3IZApXW2V92dcZf3" +
            "Pccbfemdl7t7KycuBNszbTsCZygg5sq1NTCF0ZkSGuJfmbjO9YBY2bV8H66pNdCq" +
            "72nhlD7w3fTcfpo8rmz7CzNTVg9bGILUnr7WiaC3nCxsM1EiPH/JRGSKrbA2/96B" +
            "7OWv9idOJbp/fKdub3lqzWwPtMwLWAyM/sevEqQbWOvH3sfPafYYp3iwAQmFdCJG" +
            "0zaDUnQHDFV8";
    private static final String TEST_KEY = 
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCM9Q7o6CIWAgQx" +
            "GmzeB8zWPDKJHGMTsVb/WwdrCnsuusHlnNTH5E1KAR+MEMZNV+cmVkMF9/PJIDI6" +
            "EqdZMIx2QxxoUumSaedy+F5nvWRvCb9yt2gvWjwgWyGmEQhT43KDhmrz/aYbqZht" +
            "U/S2BPKUwJ9y2TQd8+fo3+hwvkcHSplkLPjLVP2qcON0sOv2ZmESPDOakkh5dtTy" +
            "HWOVjqcJM0wk2BTbQ6fzBYtfiYpA84nUcHV+XT49fs3wIWxC2EU+7BjjEmLY5HAQ" +
            "qbK21cif/4NteZ6rGzw1vem+Wlds2JGvPq+ayhgksvHMr4N9KdIUxkP/Ll1YcqaV" +
            "++0D9wpJAgMBAAECggEBAIbisqPaD1Q0FcLPYMKPgEq6TDm8umrL106A03ZxDzPG" +
            "b2nETDIEnJlmvcORAdJdfcrH3VdG0Tjz4FyGITJOLPu2eOa0AOOlCGCSn/Rx3jwL" +
            "ffLO45n8pXDpBCggQHHWQ2ztIkfdwCjUgBKOtqu/zErgkTSb+S14i8HjElFhJCMq" +
            "qBIKFZRJTpyOC1bSWqUaG2HmPYxjsWmoh8vm2yAXGHd7oh9CZJbuxVfz4FX72457" +
            "wCgyHdsYk3T3bjh0hhEafTvUg672v5vBiZ18g9iSGxqAE0GE0SYyeWPOeTyaSUEs" +
            "Pqx+6FvLc9lQK9YD3rmsUQhLhIe9WWYEgFVyXoR4XQECgYEA1FF/gxlirDmQcu+l" +
            "VjNN4yAI/ZZVwZJyOKvSXPHvFc2P9KQ1Xo8x/OVqHsRsRUvhrlVl5/Yyl0/6/9Gw" +
            "AkyhtdAmiMtPWDpWHWb+xoY9iZQwcfKlhKqYsZrbdv0Vyih/Zfz/Xt2vdDpxCkhm" +
            "J+Wios3hulhjq6dvW3z68/wYAxkCgYEAqfUS+Roh60S5Dn2PYR9e7eyTmRWtjQMD" +
            "Q/63WbiFudBnJEkopu1PwS7Ubr7W45VVMzPsNw2p0CAcrA0CfruNW+qQbuh5VYnr" +
            "tLFEkpwGjVIOQDZjlxIvxqiZyLOek8zpDfGHT4T176qHYC47BMkUXu2POmwoeEjS" +
            "3vcQIBm21rECgYBG5MuZ/9D1xubXqfNgHBNmwlyeKCOMVhTznVPJmzME6bWmr2nu" +
            "oAvF8OiFS3sHjHXX20YCtS9hXOBnqSGHMwVQCyfCx3g/8oKsoKZzrYFAvg2LDsV7" +
            "zebcbuPXEFzFOs6HRHoUuBsMyTLThyctLT0n9Wzo5vio6WiCePHmpAd0iQKBgHIB" +
            "vbpvtzUUonZ/74AdBRbgzZmxGvwuezBPWzrKNpoQnD+Q+74ODqPt//5R8eA0OjTg" +
            "raG4fqLoB5O6HmRY91gjHsPnGg0xOXW6O9+E3jhKRNj6IxAh2P6P9qjVJJjMqcD6" +
            "teO3syoT4D+6g0iQgiOzg5KTHwqg/yMl5CFEXovhAoGBAM7+2iPRsU9NZizPTVwo" +
            "4qKdlpVxiCVy8UghqB9Xqf2v3xZD+rPMVtcjj6leTkXh9Sg50Q0ZnZw+IW+d6rHZ" +
            "agIy02D0RuAAc9nAb3diJwfUuKOzk9X8JH9c3uraajziELuInaCGzwKkKhVf3skN" +
            "JyU2YIA96sJlekUrMN7CJhBB";

    @Mock
    private OdinMaterialRetriever retriever;
    @Mock
    private MaterialPair pair;
    @Mock
    private Material publicMaterial;
    @Mock
    private Material privateMaterial;

    @Test
    public void testNewClientTls() {
        when(retriever.retrievePair(anyString())).thenReturn(pair);
        when(pair.getPublicMaterial()).thenReturn(publicMaterial);
        when(pair.getPrivateMaterial()).thenReturn(privateMaterial);
        when(publicMaterial.getMaterialType()).thenReturn(MaterialType.Certificate);
        when(publicMaterial.getMaterialData()).thenReturn(DatatypeConverter.parseBase64Binary(TEST_CERT));
        when(privateMaterial.getMaterialType()).thenReturn(MaterialType.PrivateKey);
        when(privateMaterial.getMaterialData()).thenReturn(DatatypeConverter.parseBase64Binary(TEST_KEY));

        AWSIotMqttClient client = OdinUtil.newClient(retriever, TEST_ENDPOINT, TEST_CLIENTID, TEST_MATERIAL_SET);

        assertEquals(AwsIotConnectionType.MQTT_OVER_TLS, client.getConnectionType());
    }

    @Test
    public void testNewClientWebsocket() throws UnsupportedEncodingException {
        when(retriever.retrievePair(anyString())).thenReturn(pair);
        when(pair.getPublicMaterial()).thenReturn(publicMaterial);
        when(pair.getPrivateMaterial()).thenReturn(privateMaterial);
        when(publicMaterial.getMaterialType()).thenReturn(MaterialType.Principal);
        when(publicMaterial.getMaterialData()).thenReturn(ACCESS_KEY_ID.getBytes("UTF8"));
        when(privateMaterial.getMaterialType()).thenReturn(MaterialType.Credential);
        when(privateMaterial.getMaterialData()).thenReturn(SECRET_ACCESS_KEY.getBytes("UTF8"));

        AWSIotMqttClient client = OdinUtil.newClient(retriever, TEST_ENDPOINT, TEST_CLIENTID, TEST_MATERIAL_SET);

        assertEquals(AwsIotConnectionType.MQTT_OVER_WEBSOCKET, client.getConnectionType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewClientInvalidMaterialSet() throws UnsupportedEncodingException {
        when(retriever.retrievePair(anyString())).thenReturn(pair);
        when(pair.getPublicMaterial()).thenReturn(publicMaterial);
        when(pair.getPrivateMaterial()).thenReturn(privateMaterial);
        when(publicMaterial.getMaterialType()).thenReturn(MaterialType.Principal);
        when(publicMaterial.getMaterialData()).thenReturn(DatatypeConverter.parseBase64Binary(TEST_CERT));
        when(privateMaterial.getMaterialType()).thenReturn(MaterialType.PrivateKey);
        when(privateMaterial.getMaterialData()).thenReturn(DatatypeConverter.parseBase64Binary(TEST_KEY));

        OdinUtil.newClient(retriever, TEST_ENDPOINT, TEST_CLIENTID, TEST_MATERIAL_SET);
    }

    @Test
    public void testNewMqttTlsClient() {
        AWSIotMqttClient client = OdinUtil.newMqttTlsClient(TEST_ENDPOINT, TEST_CLIENTID,
                DatatypeConverter.parseBase64Binary(TEST_CERT), DatatypeConverter.parseBase64Binary(TEST_KEY));

        assertEquals(AwsIotConnectionType.MQTT_OVER_TLS, client.getConnectionType());
    }

    @Test
    public void testNewMqttWebsocketClient() throws UnsupportedEncodingException {
        AWSIotMqttClient client = OdinUtil.newMqttWebsocketClient(TEST_ENDPOINT, TEST_CLIENTID,
                ACCESS_KEY_ID.getBytes("UTF8"), SECRET_ACCESS_KEY.getBytes("UTF8"));

        assertEquals(AwsIotConnectionType.MQTT_OVER_WEBSOCKET, client.getConnectionType());
    }

}
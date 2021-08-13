package com.intactshow.chat;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import jakarta.xml.bind.DatatypeConverter;

public class Main {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack" , "true");
        String conf = "";
        if(args.length > 0){
            conf = args[0];
        }else{
            conf = "config.ini";
        }

        try {
            new Settings(conf);

            WSServer wsServer = new WSServer(new InetSocketAddress(Settings.WS_PORT_VALUE));
            SSLContext context = getContext();
            if(context != null){
                wsServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(getContext()));
            }
            wsServer.setConnectionLostTimeout(30);
            wsServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SSLContext getContext(){
        SSLContext context;
        String password = "";
        //String pathname = "pem";
        String pathname = "/etc/letsencrypt/live/intactshow.com/";

        try {
            context = SSLContext.getInstance("TLS");
            byte[] certBytes = parseDERFromPEM(getBytes(new File(pathname + File.separator + "fullchain.pem")),
                    "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[] keyBytes = parseDERFromPEM(
                    getBytes(new File(pathname + File.separator + "privkey.pem")),
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certBytes);
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            keyStore.setCertificateEntry("cert-alias", cert);

            keyStore.setKeyEntry("key-alias", key, password.toCharArray(), new Certificate[]{cert});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, password.toCharArray());

            KeyManager[] km = kmf.getKeyManagers();
            context.init(km, null, null);

        } catch (Exception e) {
            context = null;
            e.printStackTrace();
        }

        return context;
    }

    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private static byte[] getBytes(File file) {
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesArray;
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes)
            throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

}

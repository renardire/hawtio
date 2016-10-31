package io.hawt.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;

class SSLUtil {
    private static String[] SYS_PROPERTY_NAMES = new String[]{"custom.hawtio.ssl.keyStore", "custom.hawtio.ssl.keyStorePassword", "custom.hawtio.ssl.trustStore", "custom.hawtio.ssl.trustStorePassword"};
    protected static Log log = LogFactory.getLog(SSLUtil.class);

    private SSLUtil() {
    }

    private static String[] retrieveSSLConfig() {
        if (log.isInfoEnabled()) {
            log.info("Retrieving SSL configuration:");
        }
        String[] ret = new String[SYS_PROPERTY_NAMES.length];
        for (int i = 0; i < SYS_PROPERTY_NAMES.length; i++) {
            ret[i] = System.getProperty(SSLUtil.SYS_PROPERTY_NAMES[i]);
        }
        // defaults
        if (ret[0] == null) {
            ret[0] = ".keystore";
        }
        if (ret[1] == null) {
            throw new IllegalArgumentException(SYS_PROPERTY_NAMES[1] + " is required");
        }
        if (ret[2] == null) {
            ret[2] = ".truststore";
        }
        if (ret[3] == null) {
            throw new IllegalArgumentException(SYS_PROPERTY_NAMES[3] + " is required");
        }
        // log
        for (int i = 0; i < SYS_PROPERTY_NAMES.length; i++) {
            String propName = SYS_PROPERTY_NAMES[i];
            if (log.isInfoEnabled()) {
                log.info(" Value of " + propName.substring(propName.lastIndexOf(".") + 1) + " (defined by system property " + propName + ") is " + ret[i]);
            }
        }
        return ret;
    }

    static SSLContext createSSLContext() throws GeneralSecurityException, IOException {
        try {
            return SSLUtil.SSLContextHolder.CONTEXT;
        } catch (ExceptionInInitializerError e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            if (e.getCause() instanceof GeneralSecurityException) {
                throw (GeneralSecurityException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Singleton holder, dudes.
     */
    private static class SSLContextHolder {
        static final SSLContext CONTEXT;

        private SSLContextHolder() {
        }

        private static void dumpKeystoreContent(KeyStore keystore, String name) {
            try {
                System.out.println("Dumping " + name);
                Enumeration enm = keystore.aliases();
                while (enm.hasMoreElements()) {
                    String alias = (String) enm.nextElement();
                    System.out.println("Alias name:" + alias);
                    System.out.println(keystore.getCertificate(alias));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }

        private static InputStream open(String file) {
            try {
                if (new File(file).exists()) {
                    System.out.println("Loading keystore from file");
                    return new FileInputStream(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            InputStream stream = SSLUtil.class.getClassLoader().getResourceAsStream(file);
            if (stream == null) {
                throw new IllegalStateException("Cannot read file " + file);
            }
            return stream;
        }

        static {
            try {
                String[] sslConfig = retrieveSSLConfig();
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(open(sslConfig[0]), sslConfig[1].toCharArray());
//              dumpKeystoreContent(keyStore, "keystore");
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(open(sslConfig[2]), sslConfig[3].toCharArray());
//              dumpKeystoreContent(keyStore, "truststore");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, sslConfig[1].toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                CONTEXT = SSLContext.getInstance("TLSv1.2");
                CONTEXT.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            } catch (IOException | GeneralSecurityException ioe) {
                throw new ExceptionInInitializerError(ioe);
            }
        }
    }
}

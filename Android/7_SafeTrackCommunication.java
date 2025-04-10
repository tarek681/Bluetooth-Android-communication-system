package fh.kiel.interlockapp;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Context;
import android.util.Base64;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SafeTrackCommunication extends AppCompatActivity{
    protected enum Task { Nothing, Status, Logfile }
    public Context context;
    public SSLContext sslcontext =null;
    private Listener listener;
    private RequestBody fileBody1 =null;
    Task task= Task.Nothing;
    OkHttpClient client;

    TrustManager[] trustManagers;
    SSLSocketFactory sslSocketFactory;

    File file ;
    public SafeTrackCommunication(Context context,Listener listener) {
        this.listener =listener;
        this.context= context;
        SafeTrackCommunication();



    }


    protected void SafeTrackCommunication() {

        try {
            SSlConfiguration();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        client = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]).hostnameVerifier((s, sslSession) -> true).build();
        
    }
 protected OkHttpClient getClient(){
     return client;
 }


    protected Request CreateStatusRequest(){
        task= Task.Status;
        file= new File(context.getFilesDir(), GetFile());



        byte[] buffer = ReadData();
        System.out.println("ReadedBuffer: "+ new String(buffer));
        if(buffer.length<3)
            return null;
        JSONObject DeviceStatus = new JSONObject();
       
        try {
            DeviceStatus.putOpt("DeviceStatus", new JSONObject( new String(buffer)));
        } catch (JSONException e) {
            e.printStackTrace();
        }



        RequestBody fileBody = null;
        try {


            fileBody1 = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), String.valueOf(DeviceStatus.getJSONObject("DeviceStatus").getJSONObject("foxModuleStatus")));
            fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), DeviceStatus.getJSONObject("DeviceStatus").getJSONObject("interlockMessage").getString("interlockMessage"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MultipartBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("foxModuleStatus","foxModuleStatus", fileBody1)
                .addFormDataPart("interlockMessage","interlockMessage", fileBody)
                .build();

        Request request = new Request.Builder()
                .url("https://draegersafetrack-illte-test-eu.draeger.com/aus-o/interlocklteapi/external/v1/device/status")
                .addHeader("Content-Type", "multipart/form-data")
                .post(formBody).build();
        task =Task.Nothing;
        return request;


    }



    protected Request CreateLogfileRequest(){
        task=Task.Logfile;
        file= new File(context.getFilesDir(), GetFile());
        byte[] buffer = ReadData();
        System.out.println("ReadedBuffer: "+ new String(buffer));

        RequestBody  fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), buffer);

        MultipartBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("foxModuleStatus","foxModuleStatus", fileBody)
                .addFormDataPart("interlockMessage","interlockMessage", fileBody)
                .build();

        Request request = new Request.Builder()
                .url("https://draegersafetrack-illte-test-eu.draeger.com/aus-o/interlocklteapi/external/v1/logfiles")
                .addHeader("Content-Type", "multipart/form-data")
                .post(formBody).build();
          task =Task.Nothing;
        return request;

    }


    protected byte[] ReadData(){
        byte[] buffer;
        byte [] faluire = new byte[0];
        try {
            FileInputStream fileOutputStream = new FileInputStream(file);
            int size = 0;

            try {
                size = fileOutputStream.available();
                buffer = new byte[size];
                fileOutputStream.read(buffer);
                fileOutputStream.close();
                System.out.println("no file error");
                return buffer;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("fehler e");
            }
        } catch (FileNotFoundException e) {
            System.out.println("fehler f");
            e.printStackTrace();
        }
        return faluire;
    }


    private String GetFile(){
        if(task==Task.Status)
            return Constants.StatusFile;
        else
            return Constants.LogfileFile;
    }




    void SSlConfiguration() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        InputStream trust_Cert_Content;

        trust_Cert_Content = context.getAssets().open("interlocklteapi-ca.pem");

        Certificate trust_Cert = certificateFactory.generateCertificate(trust_Cert_Content);
        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null);
        trustStore.setCertificateEntry("server-certificate", trust_Cert);

        byte[] buffer;
        try {
            InputStream is = context.getAssets().open("interlocklteapi-client-test.key");

            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);

            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String Key = new String(buffer);

        PrivateKey MYkey = getKey(Key);


        InputStream Client_Cert_Content;
        Client_Cert_Content = context.getAssets().open("interlocklteapi-client-test.pem");


        Certificate Client_Cert = certificateFactory.generateCertificate(Client_Cert_Content);


        KeyStore identityStore = KeyStore.getInstance("pkcs12");
        identityStore.load(null, null);
        identityStore.setKeyEntry("client", MYkey, "secret".toCharArray(), new Certificate[]{Client_Cert});


        trust_Cert_Content.close();
        Client_Cert_Content.close();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(identityStore, "secret".toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(keyManagers, trustManagers, null);

        sslSocketFactory = sslcontext.getSocketFactory();

    }
    public static PrivateKey getKey(String mKey){
        try{
            // convert key to String
            StringBuilder pkcs8Lines = new StringBuilder();
            BufferedReader rdr = new BufferedReader(new StringReader(mKey));
            String line;
            while ((line = rdr.readLine()) != null) {
                pkcs8Lines.append(line);
            }

            // Key as one line

            String pkcs8Pem = pkcs8Lines.toString().replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "").replaceAll("\\s+","");
            

            byte [] pkcs8EncodedBytes = android.util.Base64.decode(pkcs8Pem, Base64.DEFAULT);
            

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey RSAKey = kf.generatePrivate(keySpec);

            return RSAKey;
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return null;
    }
}

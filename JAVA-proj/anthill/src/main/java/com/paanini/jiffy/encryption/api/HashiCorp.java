package com.paanini.jiffy.encryption.api;

import com.option3.docube.schema.nodes.EncryprionAlgorithms;
import com.paanini.jiffy.exception.ProcessingException;

import com.paanini.jiffy.exception.VaultException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HashiCorp implements Vault {
    private static final String AUTH_APPROLE_ROLE = "/auth/approle/role/";
    public static final String TABLE_MODE = "tableMode";
    private String URL;
    private String rootToken;
    private String role = "role";
    private String policy = "encrypt";
    private String loginToken;
    private String AES = "aes256-gcm96";
    private String RSA = "rsa-2048";


    public String getLoginToken() {
        return loginToken;
    }

    public void setLoginToken(String loginToken) {
        this.loginToken = loginToken;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setRootToken(String rootToken) {
        this.rootToken = rootToken;
    }

    public void init() throws VaultException {
        try {
            //createRole(role);
        } catch (Exception e){
            throw new VaultException(e.getMessage());
        }
    }

    public void login() throws VaultException {
        try {
            String roleId = getRoleId(role);
            String secretId = getSecretId(role);
            String token = loginToApp(roleId,secretId);
            setLoginToken(token);
        } catch (Exception e){
            throw new VaultException(e.getMessage());
        }
    }

    private String loginToApp(String  roleId,String secretId) throws ParseException, VaultException {
        String requestUrl = new StringBuilder(URL)
                .append("/auth/approle/login")
                .toString();
        String postData =  "{ \"role_id\" : \""+roleId+"\",\"secret_id\" : \""+secretId+"\"}";
        String response = request(requestUrl, postData, "POST");
        JSONParser jsonParser = new JSONParser();
        JSONObject parse = (JSONObject)jsonParser.parse(response);
        JSONObject data =  (JSONObject) parse.get("auth");
        return data.get("client_token").toString();
    }

    private String getSecretId(String role) throws ParseException, VaultException {
        String requestUrl = new StringBuilder(URL)
                .append(AUTH_APPROLE_ROLE)
                .append(role)
                .append("/secret-id")
                .toString();
        String postData =  "{ \"metadata\" : \"\"}";
        String response = request(requestUrl, postData, "POST");
        JSONParser jsonParser = new JSONParser();
        JSONObject parse = (JSONObject)jsonParser.parse(response);
        JSONObject data =  (JSONObject) parse.get("data");
        return data.get("secret_id").toString();
    }

    private String getRoleId(String roleName) throws ParseException {
        String requestUrl = new StringBuilder(URL)
                .append(AUTH_APPROLE_ROLE)
                .append(roleName)
                .append("/role-id")
                .toString();
        StringBuffer response = getResponse(requestUrl);
        JSONParser jsonParser = new JSONParser();
        JSONObject parse = (JSONObject)jsonParser.parse(response.toString());
        JSONObject data =  (JSONObject) parse.get("data");
        String roleId = data.get("role_id").toString();
        return roleId;
    }

    private StringBuffer getResponse(String requestUrl) {
        HttpURLConnection conn = null;
        StringBuffer response = new StringBuffer();
        try{
            conn = createHttpConnection(requestUrl, "GET");
            if (conn.getResponseCode() != Response.Status.OK.getStatusCode()) {
                throw new ProcessingException("Request Failed");
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    response.append(line);
                }
            }
        }catch (Exception e){
            throw new ProcessingException(e.getMessage());
        }finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    public void createKey(String key, EncryprionAlgorithms encryprionAlgorithm) throws VaultException {
        String requestUrl = new StringBuilder(URL)
                .append("/transit/keys/")
                .append(key)
                .toString();
        String encAlg = encryprionAlgorithm.equals(EncryprionAlgorithms.RSA2048) ? RSA : AES;
        String postData =  "{ \"type\" : \""+encAlg+"\"}";
        request(requestUrl, postData,"POST");
    }

    String encrypt( String key,String data) throws VaultException {
        String requestUrl = new StringBuilder(URL)
                .append("/transit/encrypt/")
                .append(key)
                .toString();
        String encodedString = new String(Base64.getEncoder().encode(data.getBytes()));
        String postData = "{ \"plaintext\" : \""+encodedString+"\"}";
        return request(requestUrl, postData,"POST");
    }

    String decrypt(String key,String data) throws VaultException {
        String requestUrl = new StringBuilder(URL)
                .append("/transit/decrypt/")
                .append(key)
                .toString();
        String postData = "{ \"ciphertext\" : \""+data+"\"}";
        return request(requestUrl, postData,"POST");
    }

    @Override
    public String insert(VaultInput input) throws VaultException {
        createKey(input.getKey(),((HashiCorpInput)input).getEncryprionAlgorithm());
        String response = encrypt(input.getKey(),input.getValue());
        JSONParser jsonParser = new JSONParser();
        JSONObject parse = null;
        try {
            parse = (JSONObject)jsonParser.parse(response);
            JSONObject data =  (JSONObject) parse.get("data");
            return data.get("ciphertext").toString();
        } catch (ParseException e) {
            delete(input.getKey());
            throw new VaultException(e);
        }
    }

    @Override
    public String get(VaultInput input) throws VaultException {
        String res =  decrypt(input.getKey(),input.getValue());
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject parse1 =  (JSONObject)jsonParser.parse(res);
            JSONObject data1 =  (JSONObject) parse1.get("data");
            String plainText = data1.get("plaintext").toString();
            return new String(Base64.getDecoder().decode(plainText));
        } catch (ParseException e) {
            throw new VaultException(e);
        }
    }

    @Override
    public void delete(String key) throws VaultException {
        String request =  new StringBuilder(URL)
                .append("/transit/keys/")
                .append(key)
                .toString();
        HttpURLConnection conn = null;
        try {
            conn = createHttpConnection(request, "DELETE");
            if (conn.getResponseCode() != Response.Status.OK.getStatusCode()) {
                throw new VaultException("Not able to delete");
            }
        } catch (Exception e){
            throw new VaultException(e);
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public String update(String key, String value) throws VaultException {
        try {
            String response = encrypt(key,value);
            JSONParser jsonParser = new JSONParser();
            JSONObject parse = (JSONObject)jsonParser.parse(response);
            JSONObject data =  (JSONObject) parse.get("data");
            return data.get("ciphertext").toString();
        } catch (Exception e) {
            throw new VaultException(e);
        }
    }

    private void createRole(String name) throws VaultException {
        String requestUrl =  new StringBuilder(URL)
                .append(AUTH_APPROLE_ROLE)
                .append(name)
                .toString();
        String postData = "{ \"policies\" : \""+policy+"\"}";
        String s = request(requestUrl, postData,"POST");
    }

    private String request(String request, String jsonData,String method) throws VaultException {
        try {
            java.net.URL url = new URL(request);
            byte[] postData = jsonData.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Vault-Token", rootToken);
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postData, 0, postDataLength);
                }
                StringBuffer response = new StringBuffer();
                if (conn.getResponseCode() >= Response.Status.OK.getStatusCode() && conn.getResponseCode() < Response.Status.MOVED_PERMANENTLY.getStatusCode()) {

                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        for (String line = in.readLine(); line != null; line = in.readLine()) {
                            response.append(line);
                        }
                    }
                } else {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        for (String line = in.readLine(); line != null; line = in.readLine()) {
                            response.append(line);
                        }
                    }
                    throw new VaultException(response.toString());
                }

                return response.toString();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (IOException e) {
            throw new VaultException(e);
        }
    }

    private HttpURLConnection createHttpConnection(String request,String method) throws IOException {
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("X-Vault-Token",rootToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setInstanceFollowRedirects(false);
        return conn;
    }
}

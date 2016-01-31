package commons;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class APIHandler {

    public static final int MAX_ATTEMPTS = 10;
    private String baseURL;
    private String apiUser;
    private String apiToken;

    public APIHandler(
            final String url,
            final String apiUser,
            final String apiToken) {

        this.baseURL = url;
        this.apiUser = apiUser;
        this.apiToken = apiToken;
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void setApiData(
            final String urlPath,
            final Map<String, String> dataList,
            final int attempts
    ) throws OSSUSNoAPIConnectionException {

        if (attempts > MAX_ATTEMPTS) {
            throw new OSSUSNoAPIConnectionException("Could not set api data");
        }

        boolean successfullyReadAndClosed = true;

        OutputStreamWriter wr = null;
        InputStream content = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader in = null;

        try {

            URL url = new URL(this.baseURL + urlPath);

            StringBuilder dataBuilder = new StringBuilder();

            dataBuilder
                    .append(URLEncoder.encode("datetime", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(this.getDateTime(), "UTF-8"))
                    .append("&api_user=")
                    .append(apiUser)
                    .append("&api_token=")
                    .append(apiToken);


            for (Entry<String, String> pairs : dataList.entrySet()) {
                dataBuilder
                        .append("&")
                        .append(URLEncoder.encode(pairs.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(pairs.getValue(), "UTF-8"));
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            wr = new OutputStreamWriter(
                    connection.getOutputStream(),
                    StandardCharsets.UTF_8
            );
            wr.write(dataBuilder.toString());
            wr.flush();

            content = connection.getInputStream();

            inputStreamReader = new InputStreamReader(content, StandardCharsets.UTF_8);
            in = new BufferedReader(inputStreamReader);

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                result.append(line);
            }

            if (result.toString().equals("")) {
                throw new OSSUSNoAPIConnectionException("Cant write data to API");
            }

        } catch (OSSUSNoAPIConnectionException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (wr != null) {
                    wr.close();
                }
                if (content != null) {
                    content.close();
                }
                if (in != null) {
                    in.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                successfullyReadAndClosed = false;
            }
        }

        if (!successfullyReadAndClosed) {
            throw new OSSUSNoAPIConnectionException("Cannot write data to api");
        }
    }

    public void setApiData(
            final String urlPath,
            final Map<String, String> dataList
    ) throws OSSUSNoAPIConnectionException {
        setApiData(urlPath, dataList, 0);
    }

    private String downloadDataFromUrl(
            final String u
    ) throws OSSUSNoAPIConnectionException {

        boolean validReadData = true;

        StringBuilder result = new StringBuilder();
        String finalPath = u;

        HttpURLConnection connection = null;
        InputStream content = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader in = null;

        try {

            finalPath += "?" + URLEncoder.encode("datetime", "UTF-8")
                    + "=" + URLEncoder.encode(this.getDateTime(), "UTF-8")
                    + "&api_user=" + apiUser + "&api_token=" + apiToken;

            URL url = new URL(finalPath);

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            content = connection.getInputStream();
            inputStreamReader = new InputStreamReader(content, StandardCharsets.UTF_8);
            in = new BufferedReader(inputStreamReader);

            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
                if (content != null) {
                    content.close();
                }
                if (in != null) {
                    in.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                validReadData = false;
            }
        }

        if (!validReadData) {
            throw new OSSUSNoAPIConnectionException("Cant download file");
        }

        return result.toString();
    }

    public List<JSONObject> getApiData(
            final String urlPath
    ) throws OSSUSNoAPIConnectionException {

        String url = this.baseURL + urlPath;
        List<JSONObject> list = new ArrayList<>();

        Object obj = JSONValue.parse(this.downloadDataFromUrl(url));

        try {
            list.add((JSONObject) obj);
        } catch (Exception e) {
            JSONArray jsonArray = (JSONArray) obj;
            for (Object jsonObject : jsonArray) {
                list.add((JSONObject) jsonObject);
            }
        }
        return list;
    }
}

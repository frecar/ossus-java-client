package commons;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.Thread.sleep;

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

        try {

            URL url = new URL(this.baseURL + urlPath);

            String data = URLEncoder.encode("datetime", "UTF-8")
                    + "=" + URLEncoder.encode(this.getDateTime(), "UTF-8")
                    + "&api_user=" + apiUser + "&api_token=" + apiToken;

            for (Entry<String, String> pairs : dataList.entrySet()) {
                data += "&" + URLEncoder.encode(pairs.getKey(), "UTF-8")
                        + "=" + URLEncoder.encode(pairs.getValue(), "UTF-8");
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(data);
            wr.flush();

            InputStream content = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));

            while (in.readLine() != null) {
                sleep(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
    ) {

        StringBuilder result = new StringBuilder();

        String finalPath = u;

        try {

            finalPath += "?" + URLEncoder.encode("datetime", "UTF-8")
                    + "=" + URLEncoder.encode(this.getDateTime(), "UTF-8")
                    + "&api_user=" + apiUser + "&api_token=" + apiToken;

            URL url = new URL(finalPath);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            InputStream content = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    public List<JSONObject> getApiData(
            final String urlPath
    ) {

        String url = this.baseURL + urlPath;
        List<JSONObject> list = new ArrayList<JSONObject>();

        Object obj = JSONValue.parse(
                this.downloadDataFromUrl(url)
        );

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

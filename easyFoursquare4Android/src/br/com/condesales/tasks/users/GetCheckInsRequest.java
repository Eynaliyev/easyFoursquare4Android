package br.com.condesales.tasks.users;

import android.os.AsyncTask;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import br.com.condesales.constants.FoursquareConstants;
import br.com.condesales.listeners.RequestListener;
import br.com.condesales.models.Checkin;
import br.com.condesales.models.FoursquareError;


public class GetCheckInsRequest extends AsyncTask<String, Integer, ArrayList<Checkin>> {

    private RequestListener<ArrayList<Checkin>> mListener;
    private String mUserID = "self";// default value
    private FoursquareError foursquareError;

    /**
     * Async constructor (userID gonna be self)
     *
     * @param listener the listener where the async request shoud respont to
     */
    public GetCheckInsRequest(RequestListener<ArrayList<Checkin>> listener) {
        mListener = listener;
    }

    /**
     * Async constructor
     *
     * @param userID   The id from user to get information
     * @param listener the listener where the async request shoud respont to
     */
    public GetCheckInsRequest(String userID, RequestListener<ArrayList<Checkin>> listener) {
        mListener = listener;
        mUserID = userID;
    }

    /**
     * Sync constructor (userID gonna be self)
     */
    public GetCheckInsRequest() {
    }

    /**
     * Sync constructor
     *
     * @param userID The id from user to get information
     */
    public GetCheckInsRequest(String userID) {
        mUserID = userID;
    }

    @Override
    protected ArrayList<Checkin> doInBackground(String... params) {

        String access_token = params[0];
        Checkin checkin = null;
        ArrayList<Checkin> list = new ArrayList<Checkin>();
        try {
            // date required
            String apiDateVersion = FoursquareConstants.API_DATE_VERSION;
            // Call Foursquare to post checkin
            JSONObject venuesJson = executeHttpGet("https://api.foursquare.com/v2/users/"
                    + mUserID
                    + "/checkins"
                    + "?v="
                    + apiDateVersion
                    + "&oauth_token=" + access_token);

            Gson gson = new Gson();
            // Get return code
            int returnCode = Integer.parseInt(venuesJson.getJSONObject("meta")
                    .getString("code"));
            // 200 = OK
            if (returnCode == HttpStatus.SC_OK) {

                JSONArray json = venuesJson.getJSONObject("response")
                        .getJSONObject("checkins").getJSONArray("items");
                for (int i = 0; i < json.length(); i++) {
                    checkin = gson.fromJson(json.get(i).toString(),
                            Checkin.class);
                    list.add(checkin);
                }
            } else {
                String errorString = venuesJson.getJSONObject("meta").toString();
                foursquareError = gson.fromJson(errorString, FoursquareError.class);
            }

        } catch (Exception exp) {
            exp.printStackTrace();
            foursquareError = new FoursquareError();
            foursquareError.setErrorDetail(exp.getMessage());
        }
        return list;
    }

    @Override
    protected void onPostExecute(ArrayList<Checkin> checkinsList) {
        if (mListener != null)
            if (foursquareError == null) {
                mListener.onSuccess(checkinsList);
            } else {
                mListener.onError(foursquareError);
            }
        super.onPostExecute(checkinsList);
    }

    // Calls a URI and returns the answer as a JSON object
    private JSONObject executeHttpGet(String uri) throws Exception {
        HttpGet req = new HttpGet(uri);

        HttpClient client = new DefaultHttpClient();
        HttpResponse resLogin = client.execute(req);
        BufferedReader r = new BufferedReader(new InputStreamReader(resLogin
                .getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        String s = null;
        while ((s = r.readLine()) != null) {
            sb.append(s);
        }

        return new JSONObject(sb.toString());
    }
}

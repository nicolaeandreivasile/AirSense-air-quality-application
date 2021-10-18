package com.test.bluetoothlowenergyapplication.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.model.Measurement;
import com.test.bluetoothlowenergyapplication.control.ui.map.MapViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestService {
    private final static String HTTP_SERVICE_TAG = HttpRequestService.class.getSimpleName();

    public final static String MEASUREMENT = "MEASUREMENT";

    private final static String LOCATION = "location";
    private final static String TEMPERATURE = "temperature";
    private final static String HUMIDITY = "humidity";
    private final static String PRESSURE = "pressure";
    private final static String GAS = "gas";
    private final static String LIGHT = "light";
    private final static String CREATED_AT = "createdAt";

    private final static int REQUEST_TIMEOUT_MS = 5000;
    private final static int REQUEST_MAX_RETRIES = 4;

    private static HttpRequestService httpRequest;

    private RequestQueue requestQueue;
    private static Context context;

    private HttpRequestService(Context context) {
        this.context = context;
        requestQueue = getRequestQueue();
    }

    public static HttpRequestService getInstance(Context context) {
        if (httpRequest == null)
            httpRequest = new HttpRequestService(context);

        return httpRequest;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        return requestQueue;
    }

    public <T> void scheduleRequest(Request<T> request) {
        getRequestQueue().add(request);
    }

    /* Request all measurements stored in cloud */
    public void getMeasurementsHttpRequest(MapViewModel mapViewModel) {
        String url = context.getResources().getString(R.string.cloud_base_url) +
                context.getResources().getString(R.string.cloud_measurements_service_mapping) +
                context.getResources().getString(R.string.cloud_get_measurements_mapping);

        JsonArrayRequest jsonArrayRequest =
                new JsonArrayRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                Log.e(HTTP_SERVICE_TAG, "HTTP response: \n" +
                                        response.toString());

                                for (int index = 0; index < response.length(); index++) {
                                    try {
                                        JSONObject jsonObject = response.getJSONObject(index);
                                        SimpleDateFormat simpleDateFormat =
                                                new SimpleDateFormat(Measurement.DATE_PATTERN);
                                        Measurement measurement = new Measurement(
                                                jsonObject.getString(LOCATION),
                                                jsonObject.getString(GAS),
                                                jsonObject.getString(TEMPERATURE),
                                                jsonObject.getString(HUMIDITY),
                                                jsonObject.getString(PRESSURE),
                                                jsonObject.getString(LIGHT),
                                                simpleDateFormat.parse(
                                                        jsonObject.getString(CREATED_AT)));

                                        Intent measurementIntent = new Intent();
                                        measurementIntent.putExtra(MEASUREMENT, measurement);
                                        mapViewModel.selectIntent(measurementIntent);
                                    } catch (JSONException exception) {
                                        exception.printStackTrace();
                                    } catch (ParseException exception) {
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(HTTP_SERVICE_TAG, "HTTP error: " + error.toString());
                    }
                });

        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS,
                REQUEST_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        HttpRequestService.getInstance(context).scheduleRequest(jsonArrayRequest);
    }

    /* Request only measurements that are in proximity of the given location */
    public void getAreaMeasurementsHttpRequest(MapViewModel mapViewModel,
                                               String location, String radius) {
        String url = context.getResources().getString(R.string.cloud_base_url) +
                context.getResources().getString(R.string.cloud_measurements_service_mapping) +
                context.getResources().getString(R.string.cloud_get_area_measurements_mapping) +
                "/" + location + ":" + radius;

        JsonArrayRequest jsonArrayRequest =
                new JsonArrayRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                Log.e(HTTP_SERVICE_TAG, "HTTP response: \n" +
                                        response.toString());

                                for (int index = 0; index < response.length(); index++) {
                                    try {
                                        JSONObject jsonObject = response.getJSONObject(index);
                                        SimpleDateFormat simpleDateFormat =
                                                new SimpleDateFormat(Measurement.DATE_PATTERN);
                                        Measurement measurement = new Measurement(
                                                jsonObject.getString(LOCATION),
                                                jsonObject.getString(GAS),
                                                jsonObject.getString(TEMPERATURE),
                                                jsonObject.getString(HUMIDITY),
                                                jsonObject.getString(PRESSURE),
                                                jsonObject.getString(LIGHT),
                                                simpleDateFormat.parse(
                                                        jsonObject.getString(CREATED_AT)));

                                        Intent measurementIntent = new Intent();
                                        measurementIntent.putExtra(MEASUREMENT, measurement);
                                        mapViewModel.selectIntent(measurementIntent);
                                    } catch (JSONException exception) {
                                        exception.printStackTrace();
                                    } catch (ParseException exception) {
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(HTTP_SERVICE_TAG, "HTTP error: " + error.toString());
                    }
                });

        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS,
                REQUEST_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        HttpRequestService.getInstance(context).scheduleRequest(jsonArrayRequest);
    }

    /* Request a specific measurement */
    public Measurement getMeasurementHttpRequest(String location) {
        Measurement measurement = new Measurement();

        // TODO: to be implemented

        return measurement;
    }

    /* Register a new measurement */
    public void registerMeasurementHttpRequest(Measurement measurement) {
        if (measurement == null)
            return;

        String url = context.getResources().getString(R.string.cloud_base_url) +
                context.getResources().getString(R.string.cloud_measurements_service_mapping) +
                context.getResources().getString(R.string.cloud_register_measurement_mapping);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Measurement.DATE_PATTERN);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(LOCATION, measurement.getLocation());
            jsonObject.put(GAS, measurement.getGasValue());
            jsonObject.put(TEMPERATURE, measurement.getTemperatureValue());
            jsonObject.put(HUMIDITY, measurement.getHumidityValue());
            jsonObject.put(PRESSURE, measurement.getPressureValue());
            jsonObject.put(LIGHT, measurement.getLightValue());
            jsonObject.put(CREATED_AT, simpleDateFormat.format(measurement.getCreatedAt()));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e(HTTP_SERVICE_TAG, "HTTP response: \n" + response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(HTTP_SERVICE_TAG, "HTTP error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");

                return headers;
            }


        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS,
                REQUEST_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        HttpRequestService.getInstance(context).scheduleRequest(jsonObjectRequest);
    }

    /* Update a specific measurement */
    public void updateMeasurementHttpRequest(Measurement measurement) {

        // TODO: to be implemented

    }

    /* Delete a specific measurement */
    public void deleteMeasurementHttpRequest(String location) {

        // TODO: to be implemented

    }
}

package com.example.bringoz;

import java.util.HashMap;
import java.util.Map;

public class Driver {

    String id = "";
    private String status = "";
    private Map<String, Double> currentLocation = new HashMap<>();
    private final Map<String, Object> personalInformation = new HashMap<>();
    private final Map<String, Integer> workingHours = new HashMap<>();


    public Driver(String name, int age, String address, String status, double longitude, double latitude, int startTime, int endTime) {
        Map<String, Double> currentLocation = new HashMap<>();
        currentLocation.put("long", longitude);
        currentLocation.put("lat", latitude);
        this.status = status;
        this.currentLocation = currentLocation;
        personalInformation.put("Name", name);
        personalInformation.put("Age", age);
        personalInformation.put("Address", address);
        if (startTime < endTime)
            endTime += 2400;
        workingHours.put("startTime", startTime);
        workingHours.put("endTime", endTime);

    }

    public Driver() {
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getPersonalInformation() {
        return personalInformation;
    }

    public Map<String, Double> getCurrentLocation() {
        return currentLocation;
    }

    public Map<String, Integer> getWorkingHours() {
        // changing the view before the user sees it
        int endTime = workingHours.get("endTime");
        if (endTime > 2359)
            workingHours.put("endTime", endTime - 2400);
        return workingHours;
    }

    public String getId() {
        return id;
    }

    /**
     * fixing the end time before adding driver to firebase
     */
    public int getApproriateEndTime() {
        int endTime = workingHours.get("endTime");
        int startTime = workingHours.get("startTime");
        if (startTime > endTime) {
            endTime += 2400;
        }
        return endTime;
    }
}
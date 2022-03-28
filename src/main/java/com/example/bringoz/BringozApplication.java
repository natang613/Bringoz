package com.example.bringoz;


import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class BringozApplication {

    private static Firestore db;
    private static boolean firebaseInitialized = false;

    public static void main(String[] args) {
        firebaseInitialized = true;
        try {
            FileInputStream serviceAccount =
                    new FileInputStream("bringoz-df212-firebase-adminsdk-258cp-43f8443403.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
            SpringApplication.run(BringozApplication.class, args);
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    @RequestMapping(path = "drivers/post", method = {RequestMethod.POST}, consumes = "application/json")
    public Driver postDriver(@RequestBody Driver driver) throws IOException, ExecutionException, InterruptedException {
        // this is for testing purposes only
        if (!firebaseInitialized) {
            firebaseInitialized = true;
            FileInputStream serviceAccount =
                    new FileInputStream("bringoz-df212-firebase-adminsdk-258cp-43f8443403.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
        }
        ApiFuture<DocumentReference> addedDocRef = db.collection("drivers").add(driver);
        driver.id = addedDocRef.get().getId();
        return driver;
    }

    @RequestMapping(path = "driver", method = {RequestMethod.GET})
    public Driver getDriver(@RequestParam String id) throws IOException {
        DocumentReference docRef = db.collection("drivers").document(id);
        ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = docRef.get();
        try {
            Driver driver = documentSnapshotApiFuture.get().toObject(Driver.class);
            if (driver != null)
                driver.id = docRef.getId();
            return driver;
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    @RequestMapping(path = "driver/delete", method = {RequestMethod.DELETE})
    public void deleteDriver(@RequestParam String id) {
        db.collection("drivers").document(id).delete();
    }

    @RequestMapping(path = "driver/update", method = {RequestMethod.PUT}, consumes = "application/json")
    public Driver update(@RequestParam String id, @RequestBody Object updatedField) throws JSONException, IOException {
        String jsonInString = new Gson().toJson(updatedField);
        JSONObject mJSONObject = new JSONObject(jsonInString);

        if (mJSONObject.has("personalInformation") || mJSONObject.has("workingHours") || mJSONObject.has("status"))
            db.collection("drivers").document(id).update(new Gson().fromJson(mJSONObject.toString(), HashMap.class));
        return getDriver(id);
    }


    @RequestMapping(path = "drivers/active", method = {RequestMethod.GET})
    public List<Driver> getActiveDrivers() {
        ApiFuture<QuerySnapshot> querySnapshotApiFuture = db.collection("drivers").whereEqualTo("status", "active").get();
        return getFutureDrivers(querySnapshotApiFuture);
    }

    /**
     * Getting the drivers in a certain square area by lat and long assuming that the input is a box starting from
     * left corner and going to the corner below that then to the one across and finally and top one
     * |
     * 1       |   4
     * ----------------------------
     * 2      |   3
     * |
     *
     * @param long1
     * @param lat1
     * @param long2
     * @param lat2
     * @param long3
     * @param lat3
     * @param long4
     * @param lat4
     * @return
     */
    @RequestMapping(path = "drivers/location", method = {RequestMethod.GET})
    public List<Driver> getCurrentLocationDrivers(@RequestParam double long1, @RequestParam double lat1,
                                                  @RequestParam double long2, @RequestParam double lat2,
                                                  @RequestParam double long3, @RequestParam double lat3,
                                                  @RequestParam double long4, @RequestParam double lat4) {

        // validating input correctly
        if (long1 > long4 || lat1 < lat2 || long2 > long3 || lat4 < lat3) {
            return null;
        }

        // getting 2 lists of options because firebase does not allow for multi field filtering
        ApiFuture<QuerySnapshot> querySnapshotApiFuture = db.collection("drivers")
                .whereGreaterThan("currentLocation.long", long1)
                .whereGreaterThan("currentLocation.long", long2)
                .whereLessThan("currentLocation.long", long3)
                .whereLessThan("currentLocation.long", long4)
                .get();
        List<Driver> longAppropriateDrivers = getFutureDrivers(querySnapshotApiFuture);
        querySnapshotApiFuture = db.collection("drivers")
                .whereLessThan("currentLocation.lat", lat1)
                .whereGreaterThan("currentLocation.lat", lat2)
                .whereGreaterThan("currentLocation.lat", lat3)
                .whereLessThan("currentLocation.lat", lat4)
                .get();
        List<Driver> latAppropriateDrivers = getFutureDrivers(querySnapshotApiFuture);
        return listIntersection(latAppropriateDrivers, longAppropriateDrivers);

    }

    private List<Driver> listIntersection(List<Driver> latAppropriateDrivers, List<Driver> longAppropriateDrivers) {
        Set<String> ids = longAppropriateDrivers.stream().map(obj -> obj.id).collect(Collectors.toSet());
        return latAppropriateDrivers.stream()
                .filter(obj -> ids.contains(obj.id))
                .collect(Collectors.toList());
    }

    private List<Driver> getFutureDrivers(ApiFuture<QuerySnapshot> querySnapshotApiFuture) {
        List<Driver> drivers = new LinkedList<>();
        List<QueryDocumentSnapshot> documents = null;
        try {
            documents = querySnapshotApiFuture.get().getDocuments();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return drivers;
        }
        for (DocumentSnapshot document : documents) {
            Driver driver = document.toObject(Driver.class);
            if (driver != null) {
                drivers.add(driver);
                driver.id = document.getId();
            }
        }
        return drivers;
    }

    /**
     * Finding drivers who work in a certain time slot
     *
     * @param startTime The start time in 24 hour period 00:00 - 23:59
     * @param endTime   The end time in 24 hour period 00:00 - 23:59
     * @return
     */

    @RequestMapping(path = "drivers/time", method = {RequestMethod.GET})
    public List<Driver> getDriversWorkingInThisSlotOfTime(@RequestParam int startTime, @RequestParam int endTime) {
        if (startTime > endTime) {
            endTime += 2400;
        }

        ApiFuture<QuerySnapshot> querySnapshotApiFuture = db.collection("drivers")
                .whereGreaterThan("workingHours.startTime", startTime)
                .get();
        List<Driver> startingBefore = getFutureDrivers(querySnapshotApiFuture);
        List<Driver> finalList = new LinkedList<>();
        // iterating for cases where the end time is before the start time 23:00-4:00

        for (Driver driver : startingBefore) {
            System.out.println("this is the current end time " + driver.getApproriateEndTime());
            System.out.println(endTime > driver.getApproriateEndTime());
            System.out.println(1159 > driver.getApproriateEndTime());

            if (endTime > driver.getApproriateEndTime()) {
                finalList.add(driver);
            }
        }
        return finalList;
    }


}
            
package com.example.bringoz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest(BringozApplication.class)
class BringozApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void contextLoads() {
    }

    @Test
    void getDriver() throws Exception {
        String id;
        String status;
        String name;
        try {
            MockHttpServletResponse content = postTestDriver("Fred", 1000, Optional.empty(), Optional.empty());
            JSONObject jsonObj = new JSONObject(content.getContentAsString());
            id = jsonObj.getString("id");
            status = jsonObj.getString("status");
            name = jsonObj.getJSONObject("personalInformation").getString("Name");
            Assert.assertEquals("active", status);
        } catch (Exception e) {
            System.err.println("Error running the test " + e.getMessage());
            return;
        }
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/driver")
                .param("id", id)).andReturn();

        MockHttpServletResponse content = result.getResponse();
        JSONObject jsonObj = new JSONObject(content.getContentAsString());
        Assert.assertEquals(name, jsonObj.getJSONObject("personalInformation").getString("Name"));
        Assert.assertEquals(id, jsonObj.getString("id"));
        Assert.assertEquals(status, jsonObj.getString("status"));
    }

    @Test
    void postDriver() {
        Driver driver = new Driver("Mean", 23, "213 trea", "active",
                23.333, 12.111, 1000, 1200);
        try {
            MvcResult result = mvc.perform(MockMvcRequestBuilders
                    .post("/drivers/post").content(asJsonString(driver)).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();

            MockHttpServletResponse content = result.getResponse();
            Assert.assertEquals(200, content.getStatus());
            JSONObject jsonObj = new JSONObject(content.getContentAsString());
            Assert.assertEquals("Mean", jsonObj.getJSONObject("personalInformation").getString("Name"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("this is the error " + e.getMessage());
        }
    }

    public static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void update() throws Exception {
        String id;
        String status;
        try {
            MockHttpServletResponse content = postTestDriver("Henry", 900, Optional.empty(), Optional.empty());
            JSONObject jsonObj = new JSONObject(content.getContentAsString());
            id = jsonObj.getString("id");
            status = jsonObj.getString("status");
            Assert.assertEquals("active", status);
        } catch (Exception e) {
            System.err.println("Error running the test " + e.getMessage());
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "stopped");
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put("/driver/update")
                .param("id", id).content(String.valueOf(jsonObject)).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andReturn();

        MockHttpServletResponse content = result.getResponse();
        JSONObject jsonObj = new JSONObject(content.getContentAsString());
        Assert.assertEquals(id, jsonObj.getString("id"));
        Assert.assertNotEquals(status, jsonObj.getString("status"));
    }

    @Test
    void delete() throws Exception {
        String id;
        try {
            MockHttpServletResponse content = postTestDriver("Matt", 500, Optional.empty(), Optional.empty());
            JSONObject jsonObj = new JSONObject(content.getContentAsString());
            id = jsonObj.getString("id");
        } catch (Exception e) {
            System.err.println("Error running the test " + e.getMessage());
            return;
        }
        mvc.perform(MockMvcRequestBuilders.delete("/driver/delete")
                .param("id", id)).andReturn();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/driver")
                .param("id", id)).andReturn();
        Assert.assertEquals("", result.getResponse().getContentAsString());
    }

    MockHttpServletResponse postTestDriver(String name, int startTime, Optional<Double> lat, Optional<Double> longitude) throws Exception {
        Driver driver = new Driver(name, 23, "213 trea", "active",
                lat.orElse(23.333), longitude.orElse(12.011), startTime, 1158);
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .post("/drivers/post").content(asJsonString(driver)).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();

        return result.getResponse();
    }

    @Test
    void getActiveDrivers() throws Exception {
        List<String> names = new ArrayList<>();
        names.add("Barry");
        names.add("Linoel");
        names.add("Gary");
        int[] startingTime = {300, 500, 1000};
        List<Boolean> namesInReturn = new ArrayList<>();
        namesInReturn.add(false);
        namesInReturn.add(false);
        namesInReturn.add(false);
        for (int i = 0; i < 3; i++) {
            postTestDriver(names.get(i), startingTime[i], Optional.empty(), Optional.empty());
        }
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/drivers/active")).andReturn();
        MockHttpServletResponse content = result.getResponse();
        JSONArray jsonArray = new JSONArray(content.getContentAsString());
        Assert.assertNotEquals(0, jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject driver = jsonArray.getJSONObject(i);
            int index = names.indexOf(driver.getJSONObject("personalInformation").getString("Name"));
            if (index > -1 && startingTime[index] == (driver.getJSONObject("workingHours").getInt("startTime"))) {
                namesInReturn.set(index, true);
            }

        }
        Assert.assertFalse(namesInReturn.contains(false));
    }

    @Test
    void getDriversInLocation() throws Exception {
        List<String> names = new ArrayList<>();
        names.add("Barry");
        names.add("Linoel");
        names.add("Gary");
        List<Double> lats = new ArrayList<>();
        lats.add(78.11);
        lats.add(75.11);
        lats.add(22.10);
        List<Double> longs = new ArrayList<>();
        longs.add(78.00);
        longs.add(22.12);
        longs.add(81.99);
        for (int i = 0; i < 3; i++) {
            postTestDriver(names.get(i), 1000, Optional.of(lats.get(i)), Optional.of(longs.get(i)));
        }
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/drivers/location")
                .param("long1", "50.0").param("lat1", "81.0")
                .param("long2", "50.0").param("lat2", "51.0")
                .param("long3", "100.0").param("lat3", "51.0")
                .param("long4", "100.0").param("lat4", "81.0"))
                .andReturn();
        MockHttpServletResponse content = result.getResponse();
        JSONArray jsonArray = new JSONArray(content.getContentAsString());
        Assert.assertNotEquals(0, jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject driver = jsonArray.getJSONObject(i);
            int index = names.indexOf(driver.getJSONObject("personalInformation").getString("Name"));
            Assert.assertNotEquals(1, index);
            Assert.assertNotEquals(2, index);
            if (index > -1) {
                Assert.assertEquals(0, index);
            }
        }
    }

    @Test
    void getDriversWorkingInThisSlotOfTime() throws Exception {
        List<String> names = new ArrayList<>();
        names.add("Jerry");
        names.add("Jackson");
        names.add("Francis");
        int[] startingTime = {1115, 515, 1130};
        List<Boolean> namesInReturn = new ArrayList<>();
        namesInReturn.add(false);
        namesInReturn.add(false);
        namesInReturn.add(false);
        for (int i = 0; i < 3; i++) {
            postTestDriver(names.get(i), startingTime[i], Optional.empty(), Optional.empty());
        }
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/drivers/time")
                .param("startTime", "1104").param("endTime", "1159"))
                .andReturn();
        MockHttpServletResponse content = result.getResponse();
        JSONArray jsonArray = new JSONArray(content.getContentAsString());
        Assert.assertNotEquals(0, jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject driver = jsonArray.getJSONObject(i);
            int index = names.indexOf(driver.getJSONObject("personalInformation").getString("Name"));
            if (index > -1) {
                Assert.assertNotEquals(1, index);
                namesInReturn.set(index, true);
            }

        }
        Assert.assertEquals(true, namesInReturn.get(0));
        Assert.assertEquals(false, namesInReturn.get(1));
        Assert.assertEquals(true, namesInReturn.get(2));
    }

}

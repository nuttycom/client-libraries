package io.precog.api;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.precog.api.Request.ContentType;
import io.precog.api.dto.AccountInfo;
import io.precog.api.dto.IngestResult;
import io.precog.api.options.CSVIngestOptions;
import io.precog.api.options.IngestOptions;
import io.precog.json.RawStringToJson;
import io.precog.json.ToJson;
import io.precog.json.gson.GsonFromJson;
import io.precog.json.gson.GsonToJson;
import io.precog.json.gson.RawJson;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit test for basic client.
 */
public class ClientTest {

    private static String testId = null;
    private static Path testPath = null;

    public static String testAccountId;
    public static String testApiKey;

    private static class TestData {
        public final int testInt;
        public final String testStr;
        @SerializedName("~raw")
        public final RawJson testRaw;


        public TestData(int testInt, String testStr, RawJson testRaw) {
            this.testInt = testInt;
            this.testStr = testStr;
            this.testRaw = testRaw;
        }
    }

    public static final Service Local = new Service() {
        public URL serviceUrl() {
            try {
                return new URL("http", "beta.precog.com", 80, "/v1/");
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Invalid client URL", ex);
            }
        }
    };

    @BeforeClass
    public static void beforeAll() throws Exception {
        testId = "" + Double.valueOf(java.lang.Math.random() * 10000).intValue();

        Client testClient = new Client(Local, null);
        String result = testClient.createAccount("java-test@precog.com", "password");
        AccountInfo res = GsonFromJson.of(new TypeToken<AccountInfo>() {
        }).deserialize(result);
        testAccountId = res.getAccountId();
        result = testClient.describeAccount("java-test@precog.com", "password", testAccountId);
        res = GsonFromJson.of(new TypeToken<AccountInfo>() {
        }).deserialize(result);
        testApiKey = res.getApiKey();

        testPath = new Path(testAccountId).append(new Path("/test" + testId));
    }

    @Test
    public void testStore() throws IOException {
        ToJson<Object> toJson = new GsonToJson();
        Client testClient = new Client(Local, testApiKey);

        RawJson testJson = new RawJson("{\"test\":[{\"v\": 1}, {\"v\": 2}]}");
        TestData testData = new TestData(42, "Hello\" World", testJson);

        Record<TestData> testRecord = new Record<TestData>(testData);
        testClient.store(testPath, testRecord, toJson);
    }

    @Test
    public void testStoreStrToJson() throws IOException {
        ToJson<String> toJson = new RawStringToJson();
        Client testClient = new Client(Local, testApiKey);

        Record<String> testRecord = new Record<String>("{\"test\":[{\"v\": 1}, {\"v\": 2}]}");
        testClient.store(testPath, testRecord, toJson);
    }

    @Test
    public void testStoreRawString() throws IOException {
        Client testClient = new Client(Local, testApiKey);

        String rawJson = "{\"test\":[{\"v\": 1}, {\"v\": 2}]}";
        testClient.store(testPath, rawJson);
    }

    @Test
    public void testStoreRawUTF8() throws IOException {
        Client testClient = new Client(Local, testApiKey);
        String rawJson = "{\"test\":[{\"ดีลลิเชียส\": 1}, {\"v\": 2}]}";
        testClient.store(testPath, rawJson);
    }

    public static Map<String, String> jsonToStrMap(String json) {
        return GsonFromJson.of(new TypeToken<Map<String, String>>() {
        }).deserialize(json);
    }

    @Test
    public void testIngestCSV() throws IOException {
        Client testClient = new Client(Local, testApiKey);
        IngestOptions options = new CSVIngestOptions();
        String response = testClient.ingest(testPath, "blah,\n\n", options);
        IngestResult result = GsonFromJson.of(new TypeToken<IngestResult>() {
        }).deserialize(response);
        assertEquals(1, result.getIngested());
    }

    @Test
    public void testIngestJSON() throws IOException {
        Client testClient = new Client(Local, testApiKey);
        IngestOptions options = new IngestOptions(ContentType.JSON);
        String rawJson = "{\"test\":[{\"v\": 1}, {\"v\": 2}]}";
        String response = testClient.ingest(testPath, rawJson, options);
        IngestResult result = GsonFromJson.of(new TypeToken<IngestResult>() {
        }).deserialize(response);
        assertEquals(1, result.getIngested());
    }

    @Test
    public void testIngestCsvWithOptions() throws IOException {
        Client testClient = new Client(Local, testApiKey);
        CSVIngestOptions options = new CSVIngestOptions();
        options.setDelimiter(",");
        options.setQuote("'");
        options.setEscape("\\");
        String response = testClient.ingest(testPath, "blah\n\n", options);
        IngestResult result = GsonFromJson.of(new TypeToken<IngestResult>() {
        }).deserialize(response);
        assertEquals(1, result.getIngested());
    }

    @Test
    public void testIngestAsync() throws IOException {
        Client testClient = new Client(Local, testApiKey);
        IngestOptions options = new CSVIngestOptions();
        options.setAsync(true);
        String response = testClient.ingest(testPath, "blah,\n\n", options);
        //is async, so we don't expect results
        assertEquals("", response);
    }

    @Test
    public void testRawJson() throws IOException {
        ToJson<Object> toJson = new GsonToJson();

        String testString = "{\"test\":[{\"v\":1},{\"v\":2}]}";
        RawJson testJson = new RawJson(testString);
        TestData testData = new TestData(42, "Hello\" World", testJson);

        Record<TestData> testRecord = new Record<TestData>(testData);

        String expected = new StringBuilder("{")
                .append("\"testInt\":").append(42).append(",")
                .append("\"testStr\":\"Hello\\\" World\",")
                .append("\"~raw\":").append(testString)
                .append("}")
                .toString();

        assertEquals(expected, testRecord.toJson(toJson));
    }

    @Test
    public void testOddCharacters() throws IOException {
        ToJson<Object> toJson = new GsonToJson();
        TestData testData = new TestData(1, "™", new RawJson(""));

        Record<TestData> testRecord = new Record<TestData>(testData);

        String expected = new StringBuilder("{")
                .append("\"testInt\":").append(1).append(",")
                .append("\"testStr\":\"™\"")
                .append("}")
                .toString();

        String result = testRecord.toJson(toJson);

        assertEquals(expected, result);
    }


    @Test
    public void testCreateAccount() throws IOException {
        Client testClient = new Client(Local, null);
        String result = testClient.createAccount("java-test@precog.com", "password");
        assertNotNull(result);
        AccountInfo res = GsonFromJson.of(new TypeToken<AccountInfo>() {
        }).deserialize(result);
        String accountId = res.getAccountId();
        assertNotNull(accountId);
        assertNotSame("", accountId);
        assertEquals(testAccountId, accountId);
    }

    @Test
    public void testDescribeAccount() throws IOException {
        Client testClient = new Client(Local, null);
        String result = testClient.describeAccount("java-test@precog.com", "password", testAccountId);
        assertNotNull(result);
        AccountInfo res = GsonFromJson.of(new TypeToken<AccountInfo>() {
        }).deserialize(result);
        assertEquals(testAccountId, res.getAccountId());
    }

    @Test
    public void testQuery() throws IOException {
        //just test the query was sent and executed successfully
        Client testClient = new Client(Local, testApiKey);
        String result = testClient.query(new Path(testAccountId), "count(//" + testAccountId + ")");
        assertNotNull(result);
        String[] res = GsonFromJson.of(String[].class).deserialize(result);
        assertEquals("0", res[0]);
    }

}

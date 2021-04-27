package io.split.dbm.intro2api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

/**
 * Hello world!
 *
 */
public class App 
{
	public static void main( String[] args ) throws Exception
	{
		new App().execute();
	}
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	public static String API_TOKEN;
	private void execute() throws Exception {
		API_TOKEN = readFile("api_token");

		OkHttpClient client = new OkHttpClient();

		// 1) GET request to list workspaces
		Request request = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/workspaces")
				.build();

		Response workspaceResponse = client.newCall(request).execute();
		System.out.println("success getting workspaces?\t\t" + workspaceResponse.isSuccessful());

		// 2) JSON parsing to find the workspace ID for "sandbox" in result
		JSONObject workspacesObject = new JSONObject(workspaceResponse.body().string());
		JSONArray workspacesArray = workspacesObject.getJSONArray("objects");
		String workspaceIdString = "not found";
		for(int i = 0; i < workspacesArray.length(); i++) {
			JSONObject wObj = workspacesArray.getJSONObject(i);
			if(wObj.getString("name").equalsIgnoreCase("sandbox")) {
				workspaceIdString = wObj.getString("id");
				break;
			}
		}

		// 3) GET request to list environments in sandbox
		Request envRequest = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/environments/ws/" + workspaceIdString)
				.build();

		Response envResponse = client.newCall(envRequest).execute();
		System.out.println("success getting environmentments?\t" + envResponse.isSuccessful());

		// 4) JSON Parsing to grab the prod environment
		JSONArray envArray = new JSONArray(envResponse.body().string());
		String envIdString = "not found";
		for(int j = 0; j < envArray.length(); j++) {
			JSONObject eObj = envArray.getJSONObject(j);
			if(eObj.getString("name").equalsIgnoreCase("Prod-sandbox")) {
				envIdString = eObj.getString("id");
				break;
			}
		}

		// 4) Get available traffic types
		Request ttRequest = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/trafficTypes/ws/" + workspaceIdString)
				.build();

		Response ttResponse = client.newCall(ttRequest).execute();
		System.out.println("success getting traffic type?\t\t" + ttResponse.isSuccessful());
		String ttIdString = "c02daeb0-624b-11ea-af1b-0ed25467b33f"; // cheating; same parsing exercise as above

		// 5) Create a split (with no rules defined)
		RequestBody createSplitBody = RequestBody.create(JSON, readFile("intro_split.json"));
		Request creteSplitRequest = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/splits/ws/" + workspaceIdString + "/trafficTypes/" + ttIdString)
				.post(createSplitBody)
				.build();
		Response createSplitResponse = client.newCall(creteSplitRequest).execute();
		System.out.println("success creating the split?\t\t" + createSplitResponse.isSuccessful());

		Request splitsRequest = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/splits/ws/" + workspaceIdString + "?limit=50")
				.build();

		Response splitsResponse = client.newCall(splitsRequest).execute();
		System.out.println("success getting split list?\t\t" + splitsResponse.isSuccessful());

		//System.out.println(new JSONObject(splitsResponse.body().string()).toString(2));

		// Get all environments
		Request environmentsRequest = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/environments/ws/" + workspaceIdString)
				.build();

		Response environmentsResponse = client.newCall(environmentsRequest).execute();
		System.out.println("success getting environments list? " + environmentsResponse.isSuccessful());

		JSONArray environmentsArray = new JSONArray(environmentsResponse.body().string());
		//System.out.println(environmentsArray);

		List<String> environmentNames = new LinkedList<String>();
		for(int i = 0; i < environmentsArray.length(); i++) {
			environmentNames.add(environmentsArray.getJSONObject(i).getString("name"));
		}

		for(String environmentName : environmentNames) {
			// Delete rules in all environments
			Request deleteRulesInEnvironment = new Request.Builder()
					.header("Authorization", "Bearer " + API_TOKEN)
					.url("https://api.split.io/internal/api/v2/splits/ws/"+ workspaceIdString 
							+ "/" + "intro_split4" + "/environments/" + environmentName)
					.build();
			
			Response deleteRulesInEnvironmentResponse = client.newCall(deleteRulesInEnvironment).execute();
			System.out.println("delete rules in envionment " + environmentName + "? " + deleteRulesInEnvironmentResponse.isSuccessful());
		}
		// Delete split
		Request deleteSplit = new Request.Builder()
				.header("Authorization", "Bearer " + API_TOKEN)
				.url("https://api.split.io/internal/api/v2/splits/ws/" + workspaceIdString + "/intro_split4")
				.delete()
				.build();

		Response deleteSplitResponse = client.newCall(deleteSplit).execute();
		System.out.println("deleted split? " + deleteSplitResponse.isSuccessful());
		System.out.println(deleteSplitResponse.body().string());
	}

	static String readFile(String path)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}
}

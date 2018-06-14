package gov.cdc.foundation.helper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gov.cdc.helper.common.ServiceException;

public class JsonHelper {

	public Map<String, Object> flatten(JSONObject json) throws JSONException, ServiceException {
		return flatten(json, "$");
	}

	private Map<String, Object> flatten(JSONObject json, String prefix) throws JSONException, ServiceException {
		Map<String, Object> data = new HashMap<String, Object>();

		Iterator<?> it = json.keys();
		while (it.hasNext()) {
			Object key = it.next();
			Object obj = json.get(key.toString());
			
			String jsonPath = prefix + ".";
			if (key.toString().contains(" "))
				jsonPath += "'" + key.toString() + "'";
			else
				jsonPath += key.toString();
			
			if (obj instanceof JSONObject || obj instanceof JSONArray) {
				data.putAll(flatten(obj, jsonPath));
			} else {
				data.put(jsonPath, obj);
			}
		}

		return data;
	}

	private Map<String, Object> flatten(JSONArray arr, String prefix) throws JSONException, ServiceException {
		Map<String, Object> data = new HashMap<String, Object>();

		for (int i = 0; i < arr.length(); i++) {
			Object obj = arr.get(i);
			String jsonPath = prefix + "[" + i + "]";
			if (obj instanceof JSONObject || obj instanceof JSONArray) {
				data.putAll(flatten(obj, jsonPath));
			} else {
				data.put(jsonPath, obj);
			}
		}

		return data;
	}

	private Map<String, Object> flatten(Object obj, String prefix) throws JSONException, ServiceException {
		if (obj instanceof JSONObject) {
			return flatten((JSONObject) obj, prefix);
		} else if (obj instanceof JSONArray) {
			return flatten((JSONArray) obj, prefix);
		} else
			throw new ServiceException("Impossible to flatten this object type: " + obj.getClass());
	}

}

package gov.cdc.foundation.helper;

import java.util.HashMap;
import java.util.Map;

import gov.cdc.helper.AbstractMessageHelper;

public class MessageHelper extends AbstractMessageHelper {

	public static final String CONST_CONFIG = "config";
	
	public static final String METHOD_INDEX = "index";
	public static final String METHOD_UPSERTCONFIG = "upsertConfig";
	public static final String METHOD_GETCONFIG = "getConfig";
	public static final String METHOD_DELETECONFIG = "deleteConfig";
	public static final String METHOD_FLATTEN = "flatten";
	public static final String METHOD_COMBINE = "combine";
	
	public static final String ERROR_CONFIGURATION_INVALID = "The configuration name is not valid, it must match the following expression: %s";
	public static final String ERROR_CONFIG_DOESNT_EXIST = "This configuration doesn't exist.";
	public static final String ERROR_INVALID_TARGET_TYPE = "The provided target type is not valid.";
	public static final String ERROR_INVALID_ORIENTATION = "Only portrait and landscape are allowed values for the parameter orientation.";
	
	private MessageHelper() {
		throw new IllegalAccessError("Helper class");
	}

	public static Map<String, Object> initializeLog(String method, String config) {
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, method);
		if (config != null)
			log.put(MessageHelper.CONST_CONFIG, config);
		return log;
	}

}

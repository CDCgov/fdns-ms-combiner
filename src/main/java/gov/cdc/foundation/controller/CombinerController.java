package gov.cdc.foundation.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cdc.foundation.helper.CombinerHelper;
import gov.cdc.foundation.helper.JsonHelper;
import gov.cdc.foundation.helper.LoggerHelper;
import gov.cdc.foundation.helper.MessageHelper;
import gov.cdc.helper.ErrorHandler;
import gov.cdc.helper.ObjectHelper;
import gov.cdc.helper.common.ServiceException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/1.0")
public class CombinerController {

	private static final Logger logger = Logger.getLogger(CombinerController.class);
	
	@Value("${version}")
	private String version;

	@Autowired
	private CombinerHelper ch;

	private String configRegex;

	public CombinerController(@Value("${config.regex}") String configRegex) {
		this.configRegex = configRegex;
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> index() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		
		try {
			JSONObject json = new JSONObject();
			json.put("version", version);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_INDEX, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

//	@PreAuthorize(
//		"!@authz.isSecured()"
//		+ " or #config.startsWith('public-')"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.create'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.update'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.*'))"
//		+ " or #oauth2.hasScope('fdns.combiner.*.create')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.update')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.*')"
//	)
	@RequestMapping(
		value = "config/{config}",
		method = RequestMethod.PUT,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Create or update rules for the specified configuration",
		notes = "Create or update configuration"
	)
	@ResponseBody
	public ResponseEntity<?> upsertConfigWithPut(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String payload,
		@ApiParam(value = "Configuration name") @PathVariable(value = "config") String config
	) {
		return upsertConfig(authorizationHeader, payload, config);
	}

//	@PreAuthorize(
//		"!@authz.isSecured()"
//		+ " or #config.startsWith('public-')"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.create'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.update'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.*'))"
//		+ " or #oauth2.hasScope('fdns.combiner.*.create')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.update')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.*')"
//	)
	@RequestMapping(
		value = "config/{config}",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Create or update rules for the specified configuration",
		notes = "Create or update configuration"
	)
	@ResponseBody
	public ResponseEntity<?> upsertConfig(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String payload, 
		@ApiParam(value = "Configuration name") @PathVariable(value = "config") String config
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_UPSERTCONFIG, config);

		try {
			// First, check the configuration name
			Pattern p = Pattern.compile(configRegex);
			if (!p.matcher(config).matches())
				throw new ServiceException(String.format(MessageHelper.ERROR_CONFIGURATION_INVALID, configRegex));

			JSONObject data = new JSONObject(payload);
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (helper.exists(config)) {
				helper.updateObject(config, data);
			} else {
				helper.createObject(data, config);
			}

			JSONObject json = new JSONObject();
			json.put(MessageHelper.CONST_SUCCESS, true);
			json.put(MessageHelper.CONST_CONFIG, config);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_UPSERTCONFIG, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

//	@PreAuthorize(
//		"!@authz.isSecured()"
//		+ " or #config.startsWith('public-')"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.read'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.*'))"
//		+ " or #oauth2.hasScope('fdns.combiner.*.read')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.*')"
//	)
	@RequestMapping(
		value = "config/{config}",
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Get configuration",
		notes = "Get configuration"
	)
	@ResponseBody
	public ResponseEntity<?> getConfig(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@ApiParam(value = "Configuration name") @PathVariable(value = "config") String config
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_GETCONFIG, config);

		try {
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (!helper.exists(config))
				throw new ServiceException(MessageHelper.ERROR_CONFIG_DOESNT_EXIST);

			return new ResponseEntity<>(mapper.readTree(helper.getObject(config).toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_GETCONFIG, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

//	@PreAuthorize(
//		"!@authz.isSecured()"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.delete'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.*'))"
//		+ " or #oauth2.hasScope('fdns.combiner.*.delete')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.*')"
//	)
	@RequestMapping(
		value = "config/{config}",
		method = RequestMethod.DELETE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Delete configuration",
		notes = "Delete configuration"
	)
	@ResponseBody
	public ResponseEntity<?> deleteConfig(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@ApiParam(value = "Configuration name") @PathVariable(value = "config") String config
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_DELETECONFIG, config);

		try {
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (!helper.exists(config))
				throw new ServiceException(MessageHelper.ERROR_CONFIG_DOESNT_EXIST);

			helper.deleteObject(config);

			return new ResponseEntity<>(mapper.readTree("{ \"success\" : true }"), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_DELETECONFIG, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

//	@PreAuthorize(
//		"!@authz.isSecured()"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.read'))"
//		+ " or #oauth2.hasScope('fdns.combiner.'.concat(#config).concat('.*'))"
//		+ " or #oauth2.hasScope('fdns.combiner.*.read')"
//		+ " or #oauth2.hasScope('fdns.combiner.*.*')"
//	)
	@RequestMapping(
		method = RequestMethod.POST,
		value = "/{targetType}/{config}",
		produces = MediaType.TEXT_PLAIN_VALUE,
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	@ApiOperation(
		value = "Export data to CSV or XLSX",
		notes = "Parse data and transform them to CSV or XLSX\n***Multiple File Upload not working in swagger. Please" +
				" use another tool to make calls which require it.***"
	)
	@ResponseBody
	public ResponseEntity<?> transform(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestPart("file") MultipartFile[] file,
		@ApiParam(value = "Target Type", allowableValues = "csv,xlsx") @PathVariable(value = "targetType", required = true) String targetType,
		@ApiParam(value = "Combiner Configuration") @PathVariable(value = "config", required = true) String config,
		@ApiParam(value = "Filename expected") @RequestParam(value = "filename", required = false) String filename,
		@ApiParam(value = "Orientation", allowableValues = "portrait,landscape", defaultValue = "portrait") @RequestParam(value = "orientation", required = false) String orientation, 
		@ApiParam(value = "Include Header", defaultValue = "true") @RequestParam(value = "includeHeader", required = false) boolean includeHeader,
		@ApiParam(value = "Sort file names", defaultValue = "false") @RequestParam(value = "sortFiles", required = false) boolean sortFiles
	) {
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_COMBINE, config);

		try {
			if (!("csv".equalsIgnoreCase(targetType) || "xlsx".equalsIgnoreCase(targetType)))
				throw new ServiceException(MessageHelper.ERROR_INVALID_TARGET_TYPE);
			if (orientation != null && !("portrait".equalsIgnoreCase(orientation) || "landscape".equalsIgnoreCase(orientation)))
				throw new ServiceException(MessageHelper.ERROR_INVALID_ORIENTATION);

			int maxRetries = 10;
			int retry = 0;
			JSONObject configJson = null;
			while (retry < maxRetries && configJson == null)
				try {
					configJson = ObjectHelper.getInstance(authorizationHeader).getObject(config);
				} catch (HttpClientErrorException e) {
					logger.error(e);
					retry++;
				}

			if (configJson == null)
				throw new ServiceException(MessageHelper.ERROR_CONFIG_DOESNT_EXIST);

			// Get the list of labels
			List<String> labels = ch.getLabels(configJson);
			labels.add(0, "Parsed");

			// Get the list of labels
			List<String> descriptions = ch.getDescriptions(configJson);
			descriptions.add(0, "Parsed successfully?");

			// Get aggregated map
			Map<String, Map<String, String>> aggregatedMap = getAggregatedData(file, configJson);

			// Get the filename
			String generatedFilename;
			if (filename != null && !filename.isEmpty())
				generatedFilename = filename;
			else {
				generatedFilename = ch.getFilename(configJson) + "." + targetType;
			}

			if ("csv".equalsIgnoreCase(targetType)) {
				byte[] csv = ch.transformToCSV(aggregatedMap, descriptions, labels, orientation, includeHeader, sortFiles);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.TEXT_PLAIN);
				headers.setContentDispositionFormData("file", generatedFilename);
				return new ResponseEntity<>(csv, headers, HttpStatus.OK);
			} else {
				byte[] csv = ch.transformToXLSX(aggregatedMap, descriptions, labels, orientation, includeHeader, sortFiles);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
				headers.setContentDispositionFormData("file", generatedFilename);
				return new ResponseEntity<>(csv, headers, HttpStatus.OK);
			}

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_COMBINE, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "/{targetType}/flatten",
		method = RequestMethod.POST
	)
	@ApiOperation(
		value = "Flatten JSON object",
		notes = "Flatten a JSON object to JSON, CSV and XLSX"
	)
	@ResponseBody
	public ResponseEntity<?> flatten(
		@RequestPart("file") MultipartFile file,
		@ApiParam(value = "Target Type",
		allowableValues = "json,csv,xlsx") @PathVariable(value = "targetType",
		required = true) String targetType
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_FLATTEN, null);

		try {
			if (!("json".equalsIgnoreCase(targetType) || "csv".equalsIgnoreCase(targetType) || "xlsx".equalsIgnoreCase(targetType)))
				throw new ServiceException(MessageHelper.ERROR_INVALID_TARGET_TYPE);

			InputStream is = file.getInputStream();
			String incoming = IOUtils.toString(is);
			Map<String, Object> data = (new JsonHelper()).flatten(new JSONObject(incoming));

			if ("json".equalsIgnoreCase(targetType)) {
				JSONObject output = new JSONObject(data);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.setContentDispositionFormData("file", UUID.randomUUID() + ".json");
				return new ResponseEntity<>(mapper.readTree(output.toString()), headers, HttpStatus.OK);
			} else {

				// Build the list of labels
				List<String> keys = new ArrayList<>(data.keySet());
				Collections.sort(keys);

				// Build the aggregated map
				Map<String, Map<String, String>> aggregatedMap = new HashMap<>();
				Map<String, String> values = new HashMap<>();
				for (String key : keys) {
					values.put(key, data.get(key).toString());
				}
				aggregatedMap.put("Value", values);

				if ("csv".equalsIgnoreCase(targetType)) {
					byte[] csv = ch.transformToCSV(aggregatedMap, Collections.emptyList(), keys, "portrait", true, false);
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.TEXT_PLAIN);
					headers.setContentDispositionFormData("file", UUID.randomUUID() + ".csv");
					return new ResponseEntity<>(csv, headers, HttpStatus.OK);
				} else {
					byte[] csv = ch.transformToXLSX(aggregatedMap, Collections.emptyList(), keys, "portrait", true, false);
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
					headers.setContentDispositionFormData("file", UUID.randomUUID() + ".xlsx");
					return new ResponseEntity<>(csv, headers, HttpStatus.OK);
				}
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_FLATTEN, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private Map<String, Map<String, String>> getAggregatedData(
		MultipartFile[] files,
		JSONObject configJson
	) throws IOException, JSONException {
		Map<String, Map<String, String>> aggregatedMap = new LinkedHashMap<>();
		for (MultipartFile file : files) {
			boolean parsed = true;
			JSONObject json = null;
			
			try {
				// Load the message as JSON
				InputStream is = file.getInputStream();
				String message = IOUtils.toString(is);
				json = new JSONObject(message);
			} catch (Exception e) {
				logger.error("Error parsing message", e);
				parsed = false;
			}

			// Create the map
			Map<String, String> messageData = ch.initialize(configJson);

			// Save if we parsed properly the file
			messageData.put("Parsed", parsed ? "Y" : "N");

			if (parsed && json != null) {
				// Parse the JSON
				ch.populate(configJson, json, messageData);
			}

			// Save the map
			aggregatedMap.put(file.getOriginalFilename(), messageData);
		}
		return aggregatedMap;
	}

}
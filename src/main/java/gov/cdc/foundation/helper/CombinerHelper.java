package gov.cdc.foundation.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.opencsv.CSVWriter;

@Component
public class CombinerHelper {

	private char CSV_SEPARATOR;
	private String VALUE_SEPARATOR;

	private static final Logger logger = Logger.getLogger(CombinerHelper.class);

	public CombinerHelper(@Value("${csv.separator}") char CSV_SEPARATOR, @Value("${value.separator}") String VALUE_SEPARATOR) {
		this.CSV_SEPARATOR = CSV_SEPARATOR;
		this.VALUE_SEPARATOR = VALUE_SEPARATOR;
	}

	public List<String> getLabels(JSONObject configuration) throws JSONException {
		List<String> labels = new ArrayList<>();

		JSONArray rows = configuration.getJSONArray("rows");
		for (int i = 0; i < rows.length(); i++) {
			JSONObject row = rows.getJSONObject(i);
			String label = getLabel(row);
			if (label != null)
				labels.add(label);
		}

		return labels;
	}

	public List<String> getDescriptions(JSONObject configuration) throws JSONException {
		List<String> descriptions = new ArrayList<>();

		JSONArray rows = configuration.getJSONArray("rows");
		for (int i = 0; i < rows.length(); i++) {
			JSONObject row = rows.getJSONObject(i);
			String description = getDescription(row);
			if (description != null)
				descriptions.add(description);
		}

		return descriptions;
	}

	public Map<String, String> initialize(JSONObject configuration) throws JSONException {
		Map<String, String> map = new HashMap<>();
		List<String> labels = getLabels(configuration);
		for (String label : labels) {
			map.put(label, "");
		}
		return map;
	}

	private String getLabel(JSONObject row) throws JSONException {
		if (row.has("label"))
			return row.getString("label");
		else if (row.has("jsonPath"))
			return row.getString("jsonPath");
		else
			return null;
	}

	private String getDescription(JSONObject row) throws JSONException {
		if (row.has("description"))
			return row.getString("description");
		else
			return "";
	}

	private String getSeparator(JSONObject row) throws JSONException {
		if (row.has("separator"))
			return row.getString("separator");
		else
			return VALUE_SEPARATOR;
	}

	public void populate(JSONObject configuration, JSONObject json, Map<String, String> data) throws JSONException {
		JSONArray rows = configuration.getJSONArray("rows");
		for (int i = 0; i < rows.length(); i++) {
			JSONObject row = rows.getJSONObject(i);
			String label = getLabel(row);
			if (label != null && row.has("jsonPath")) {
				String jsonPath = row.getString("jsonPath");

				StringBuilder bld = new StringBuilder();
				String value = "";
				Object extract = null;
				try {
					extract = JsonPath.read(json.toString(), jsonPath);
				} catch (PathNotFoundException e) {
					logger.error(e);
					// Do nothing
				}
				if (extract != null)
					if (extract instanceof net.minidev.json.JSONArray) {
						net.minidev.json.JSONArray values = (net.minidev.json.JSONArray) extract;
						for (int j = 0; j < values.size(); j++) {
							if (j > 0)
								bld.append(getSeparator(row));
							bld.append(values.get(j).toString());
						}
						value = bld.toString();
					} else if (extract instanceof String) {
						value = (String) extract;
					} else
						value = extract.toString();
				data.put(label, value);
			}
		}
	}

	public byte[] transformToCSV(Map<String, Map<String, String>> aggregatedMap, List<String> descriptions, List<String> labels, String orientation, boolean includeHeader, boolean sortFiles) throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw, CSV_SEPARATOR);

		if (!aggregatedMap.isEmpty()) {

			List<String> fileNames = new ArrayList<>();
			for (String fileName : aggregatedMap.keySet())
				fileNames.add(fileName);
			if (sortFiles)
				Collections.sort(fileNames);

			if (orientation == null || orientation.isEmpty() || "portrait".equalsIgnoreCase(orientation)) {
				// Display the list of file names
				String[] entries;
				int i;
				if (includeHeader) {
					entries = new String[2 + fileNames.size()];
					i = 2;
				} else {
					entries = new String[fileNames.size()];
					i = 0;
				}
				for (String fileName : fileNames) {
					entries[i] = fileName;
					i++;
				}
				writer.writeNext(entries);

				// Display one line per label
				for (int rowIdx = 0; rowIdx < labels.size(); rowIdx++) {

					String label = labels.get(rowIdx);

					if (includeHeader) {
						String description = rowIdx < descriptions.size() ? descriptions.get(rowIdx) : "";
						entries = new String[2 + aggregatedMap.keySet().size()];
						entries[0] = description;
						entries[1] = label;
						i = 2;
					} else {
						entries = new String[fileNames.size()];
						i = 0;
					}

					for (String filename : fileNames) {
						entries[i] = aggregatedMap.get(filename).get(label);
						i++;
					}
					writer.writeNext(entries);
				}
			} else if ("landscape".equalsIgnoreCase(orientation)) {
				String[] entries;
				int i;
				if (includeHeader) {
					// Display the list of descriptions
					entries = new String[1 + descriptions.size()];
					i = 1;
					for (String description : descriptions) {
						entries[i] = description;
						i++;
					}
					writer.writeNext(entries);

					// Display the list of labels
					entries = new String[1 + labels.size()];
					i = 1;
					for (String label : labels) {
						entries[i] = label;
						i++;
					}
					writer.writeNext(entries);
				}

				// Display one file per line
				for (String filename : fileNames) {
					entries = new String[1 + labels.size()];
					entries[0] = filename;
					i = 1;
					for (String label : labels) {
						entries[i] = aggregatedMap.get(filename).get(label);
						i++;
					}
					writer.writeNext(entries);
				}
			}
		}

		writer.close();
		return sw.toString().getBytes();
	}

	public String getFilename(JSONObject configuration) throws JSONException {
		String template = configuration.getJSONObject("file").getString("template");
		String df = null;
		if (configuration.getJSONObject("file").has("date-format"))
			df = configuration.getJSONObject("file").getString("date-format");
		if (df != null && !df.isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat(df);
			return template.replaceAll("\\$DATE\\$", sdf.format(Calendar.getInstance().getTime()));
		} else
			return template;
	}

	public byte[] transformToXLSX(Map<String, Map<String, String>> aggregatedMap, List<String> descriptions, List<String> labels, String orientation, boolean includeHeader, boolean sortFiles) throws IOException {
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet("Data");

		if (!aggregatedMap.isEmpty()) {

			int rowNumber = 0;

			List<String> fileNames = new ArrayList<>();
			for (String fileName : aggregatedMap.keySet())
				fileNames.add(fileName);
			if (sortFiles)
				Collections.sort(fileNames);

			if (orientation == null || orientation.isEmpty() || "portrait".equalsIgnoreCase(orientation)) {
				// Display the list of file names
				Row headers = sheet.createRow(rowNumber);

				int i = includeHeader ? 2 : 0;
				for (String fileName : fileNames) {
					Cell cell = headers.createCell(i);
					cell.setCellValue(fileName);
					i++;
				}
				rowNumber++;

				// Display one line per label
				for (int rowIdx = 0; rowIdx < labels.size(); rowIdx++) {
					String label = labels.get(rowIdx);
					String description = rowIdx < descriptions.size() ? descriptions.get(rowIdx) : "";

					Row row = sheet.createRow(rowNumber);
					i = 0;
					Cell cell;

					if (includeHeader) {
						// Display description
						cell = row.createCell(i);
						cell.setCellValue(description);
						i++;

						// Display label
						cell = row.createCell(i);
						cell.setCellValue(label);
						i++;
					}

					// Display values
					for (String filename : fileNames) {
						cell = row.createCell(i);
						cell.setCellValue(aggregatedMap.get(filename).get(label));
						i++;
					}
					rowNumber++;
				}
			} else if ("landscape".equalsIgnoreCase(orientation)) {
				int i;

				if (includeHeader) {
					// Display the list of descriptions
					Row headers = sheet.createRow(rowNumber);

					i = 1;
					for (String description : descriptions) {
						Cell cell = headers.createCell(i);
						cell.setCellValue(description);
						i++;
					}
					rowNumber++;

					// Display the list of labels
					headers = sheet.createRow(rowNumber);

					i = 1;
					for (String label : labels) {
						Cell cell = headers.createCell(i);
						cell.setCellValue(label);
						i++;
					}
					rowNumber++;
				}

				// Display one file per line
				for (String filename : fileNames) {

					Row row = sheet.createRow(rowNumber);
					i = 0;

					// Display label
					Cell cell = row.createCell(i);
					cell.setCellValue(filename);
					i++;

					// Display values
					for (String label : labels) {
						cell = row.createCell(i);
						cell.setCellValue(aggregatedMap.get(filename).get(label));
						i++;
					}
					rowNumber++;
				}
			}
		}

		// Generate the file
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		wb.write(baos);

		// Close the workbook
		wb.close();

		return baos.toByteArray();
	}

}

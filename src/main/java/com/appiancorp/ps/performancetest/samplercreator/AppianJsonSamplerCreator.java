package com.appiancorp.ps.performancetest.samplercreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.extractor.BeanShellPostProcessor;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui;
import org.apache.jmeter.protocol.http.proxy.DefaultSamplerCreator;
import org.apache.jmeter.protocol.http.proxy.SamplerCreator;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.jayway.jsonpath.JsonPath;

/**
 * Handles Appian Specific Requests
 */
public class AppianJsonSamplerCreator extends DefaultSamplerCreator {
	@SuppressWarnings("unused")
	private static final Logger log = LoggingManager.getLoggerForClass();
	private static final List<String> APPIAN_CONTENT_TYPES = Arrays.asList("application/vnd.appian.tv+json", "application/vnd.appian.tv.ui+json");
	private static final List<String> STANDARD_CONTEXT_TYPES = Arrays.asList(
			"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", "*/*");

	private static Map<String, String> saveIntoMap = new HashMap<String, String>();
	private List<String> paramList;

	private static final String ESCAPE_RETURNS = "String[] params= {%s};\r\nString param = \"\";\r\nfor(i = 0; i < params.length;i++) {\r\n  param = vars.get(params[i]);\r\n  param = param.replaceAll(\"\\\\r\\\\n\", \"\\\\\\\\r\\\\\\\\n\");\r\n  vars.put(params[i], param);\r\n}";

	/**
     * 
     */
	public AppianJsonSamplerCreator() {
	}

	/**
	 * @see org.apache.jmeter.protocol.http.proxy.SamplerCreator#getManagedContentTypes()
	 */
	@Override
	public String[] getManagedContentTypes() {
		List<String> allContentTypes = new ArrayList<String>();
		allContentTypes.addAll(APPIAN_CONTENT_TYPES);
		allContentTypes.addAll(STANDARD_CONTEXT_TYPES);

		return allContentTypes.toArray(new String[allContentTypes.size()]);
	}

	/**
	 * Default implementation returns an empty list
	 * 
	 * @see SamplerCreator#createChildren(HTTPSamplerBase, SampleResult)
	 */
	@Override
	public List<TestElement> createChildren(HTTPSamplerBase sampler, SampleResult result) {

		List<TestElement> children = new ArrayList<TestElement>();

		paramList = new ArrayList<String>();

		handleTaskId(sampler, result, children);
		handleProcessModelId(sampler, result, children);
		handleDocuments(sampler, result, children);
		handleContext(sampler, result, children);
		handleSaveInto(sampler, result, children);
		handleReturns(sampler, result, children);

		return children;
	}

	private void handleTaskId(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		if (getResponse(result).contains("taskId")) {
			children.add(createJsonPostProcessor(sampler, "TASK_ID", "$.taskId"));
		}

		String samplePath = sampler.getPath();
		String out = samplePath.replaceAll("/task/latest/[0-9]+/form", "/task/latest/\\${TASK_ID}/form");
		sampler.setPath(out);
	}

	private void handleProcessModelId(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		// NOOP
	}

	private void handleDocuments(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		// Extract Document Id
		if (getResponse(result).contains("\"opaqueId\":")) {
			children.add(createRegexPostProcessor(sampler, "DOC_ID", "\"id\":([^,]+)"));
		}

		String out;

		// Parameterize Document Id
		if (sampler.hasArguments()) {
			out = getRequest(sampler).replaceAll("\"id\":[0-9]+, \"#t\":\"CollaborationDocument\"",
					"\"id\":\\${DOC_ID}, \"#t\":\"CollaborationDocument\"");
			setRequest(sampler, out);
		}

		// Parameterize Mutlipart CSRF Token
		String samplePath = sampler.getPath();
		out = samplePath.replaceAll("appian_mp_csrf=.*", "appian_mp_csrf=\\${COOKIE___appianMultipartCsrfToken}");
		sampler.setPath(out);
	}

	private void handleContext(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		if (getResponse(result).contains("context")) {
			children.add(createJsonPostProcessor(sampler, "CONTEXT", "$.context"));
			paramList.add("CONTEXT");
		}

		if (sampler.hasArguments()) {
			String jsonString = getRequest(sampler);
			String out = jsonString.replaceAll("\\{\"context\":\\{[^}]+}", "{\"context\":\\${CONTEXT}");
			setRequest(sampler, out);
		}
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void handleSaveInto(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		if (APPIAN_CONTENT_TYPES.contains(result.getContentType())) {
			JsonPath path = JsonPath.compile("$..[?(@.saveInto && @._cId)]");
			List<Object> saveIntoObjs = path.read(getResponse(result));

			for (Object o : saveIntoObjs) {
				JSONObject jo = new JSONObject((Map) o);
				String cId = (String) jo.get("_cId");
				String label = (String) jo.get("label");
				String saveInto = ((JSONArray) jo.get("saveInto")).toJSONString().replace("\\/", "/");

				saveIntoMap.put(saveInto, cId);
				paramList.add("SAVE_INTO_" + cId.toUpperCase());
				children.add(createJsonPostProcessor(sampler, "SAVE_INTO_" + cId.toUpperCase(), "$..[?(@['_cId'] == '" + cId + "')].saveInto"));
			}
		}

		if (sampler.hasArguments()) {
			String request = getRequest(sampler);
			Pattern p = Pattern.compile("\"saveInto\":([^},]+)");
			Matcher m = p.matcher(request);

			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(sb, "\"saveInto\":\\${SAVE_INTO_" + saveIntoMap.get(m.group(1)).toUpperCase() + "}");
			}
			m.appendTail(sb);

			setRequest(sampler, sb.toString());
		}
	}

	private void handleReturns(HTTPSamplerBase sampler, SampleResult result, List<TestElement> children) {
		String escapees = "";
		for (String id : paramList) {
			if (StringUtils.isNotEmpty(escapees)) {
				escapees += ", ";
			}
			escapees += "\"" + id + "\"";
		}

		if (StringUtils.isNotEmpty(escapees)) {
			children.add(createBeanShellPostProcessor(sampler, "Escape Returns", String.format(ESCAPE_RETURNS, escapees)));
		}
	}

	private TestElement createRegexPostProcessor(HTTPSamplerBase sampler, String variable, String expression) {
		RegexExtractor rgx = new RegexExtractor();
		rgx.setName("Get " + variable);
		rgx.setRefName(variable);
		rgx.setDefaultValue("${" + variable + "}");
		rgx.setRegex(expression);
		rgx.setTemplate("$1$");
		rgx.setProperty(TestElement.TEST_CLASS, RegexExtractor.class.getName());
		rgx.setProperty(TestElement.GUI_CLASS, RegexExtractorGui.class.getName());

		return rgx;
	}

	private TestElement createJsonPostProcessor(HTTPSamplerBase sampler, String variable, String expression) {
		JSONPostProcessor jpp = new JSONPostProcessor();
		jpp.setName("Get " + variable);
		jpp.setRefNames(variable);
		jpp.setDefaultValues("${" + variable + "}");
		jpp.setJsonPathExpressions(expression);
		jpp.setProperty(TestElement.TEST_CLASS, JSONPostProcessor.class.getName());
		jpp.setProperty(TestElement.GUI_CLASS, JSONPostProcessorGui.class.getName());

		return jpp;
	}

	private TestElement createBeanShellPostProcessor(HTTPSamplerBase sampler, String name, String script) {
		BeanShellPostProcessor bpp = new BeanShellPostProcessor();
		bpp.setName(name);
		bpp.setScript(script);
		bpp.setProperty("script", script);
		bpp.setProperty(TestElement.TEST_CLASS, BeanShellPostProcessor.class.getName());
		bpp.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());

		return bpp;
	}

	private String getRequest(HTTPSamplerBase sampler) {
		return sampler.getArguments().getArgument(0).getValue();
	}

	private void setRequest(HTTPSamplerBase sampler, String request) {
		sampler.getArguments().getArgument(0).setValue(request);
	}

	private String getResponse(SampleResult result) {
		return new String(result.getResponseData());
	}
}

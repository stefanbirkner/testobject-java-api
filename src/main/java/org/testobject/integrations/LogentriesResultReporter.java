package org.testobject.integrations;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class LogentriesResultReporter implements TestRule {
	private static final String LOGENTRIES_URL = "http://webhook.logentries.com/noformat/logs/";
	private static final String LOGENTRIES_TOKEN_ENV_NAME = "LOGENTRIES_TOKEN";
	private static final String TEST_IDENTIFIER_ENV_NAME = "TEST_IDENTIFIER";
	private static final String JENKINS_JOB_NAME_ENV_NAME = "JOB_NAME";
	private static final String JENKINS_BUILD_URL_ENV_NAME = "BUILD_URL";

	private static final String JSON_TEST_IDENTIFIER_FIELD = "testIdentifier";
	private static final String JSON_TEST_NAME_FIELD = "testName";
	private static final String JSON_DURATION_FIELD = "duration";
	private static final String JSON_RESULT_FIELD = "result";
	private static final String JSON_JENKINS_JOB_NAME_FIELD = "jenkinsJobName";
	private static final String JSON_JENKINS_BUILD_URL_FIELD = "jenkinsBuildUrl";

	private final Client client;
	private final WebTarget webTarget;
	private final String testIdentifier;
	private final String jenkinsJobName;
	private final String jenkinsBuildUrl;

	protected LogentriesResultReporter(Client client, String logentriesToken) {
		if (logentriesToken == null) {
			throw new NullPointerException("logentriesToken can't be null");
		}
		this.client = client;
		this.webTarget = client.target(LOGENTRIES_URL).path(logentriesToken);
		this.testIdentifier = System.getenv(TEST_IDENTIFIER_ENV_NAME);
		this.jenkinsJobName = System.getenv(JENKINS_JOB_NAME_ENV_NAME);
		this.jenkinsBuildUrl = System.getenv(JENKINS_BUILD_URL_ENV_NAME);
	}

	public LogentriesResultReporter(String logentriesToken) {
		this(ClientBuilder.newBuilder().build(), logentriesToken);
	}

	public LogentriesResultReporter() {
		this(ClientBuilder.newBuilder().build(), System.getenv(LOGENTRIES_TOKEN_ENV_NAME));
	}

	@Override
	public Statement apply(Statement base, Description description) {
		if (description.isEmpty()) {
			return base;
		} else {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					Test test = new Test(description.getDisplayName(), System.currentTimeMillis());

					try {
						base.evaluate();
						test.setState(Test.State.SUCCESS);
					} catch (@SuppressWarnings("deprecation") org.junit.internal.AssumptionViolatedException  e) {
						test.setState(Test.State.SKIPPED);
						throw e;
					} catch (Throwable e) {
						test.setState(Test.State.FAILED);
						throw e;
					} finally {
						test.setFinishTime(System.currentTimeMillis());
						sendResult(test);
						client.close();
					}
				}
			};
		}
	}

	private void sendResult(Test test) {
		long testDuration = test.getFinishTime() - test.getStartTime();
		Map<String, Object> json = new HashMap<>();
		json.put(JSON_TEST_NAME_FIELD, test.getName());
		json.put(JSON_DURATION_FIELD, testDuration);
		json.put(JSON_RESULT_FIELD, test.getState().name());
		addOptionalVariables(json);
		Response response = webTarget.request().post(Entity.json(json));
		if (response.getStatus() != 204) {
			throw new RuntimeException("Didn't receive the expected result from logentries. Result: " + response.toString());
		}
	}

	private void addOptionalVariables(Map<String, Object> json) {
		if (testIdentifier != null) {
			json.put(JSON_TEST_IDENTIFIER_FIELD, testIdentifier);
		}
		if (jenkinsJobName != null) {
			json.put(JSON_JENKINS_JOB_NAME_FIELD, jenkinsJobName);
		}
		if (jenkinsBuildUrl != null) {
			json.put(JSON_JENKINS_BUILD_URL_FIELD, jenkinsBuildUrl);
		}
	}
}

package eu.gaiax.notarization.domain;

import eu.gaiax.notarization.environment.Person;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RequestSession(
		String name,
		String sessionId,
		String token,
		String location,
		Person person,
		Profile profileId,
		List<RequestValues> values) {

	public static class RequestValues {

		public String sessionId;
		public String profileId;
		public String state;
		public Set<SessionTaskSummary> tasks;
		public SessionTaskTree preconditionTaskTree;
		public SessionTaskTree taskTree;
		public boolean preconditionTasksFulfilled;
		public boolean tasksFulfilled;
	}

	public static class SessionTaskTree {

		public SessionTaskSummary task;
		public Set<SessionTaskTree> allOf;
		public Set<SessionTaskTree> oneOf;
	}

	public static class SessionTaskSummary {

		public UUID taskId;

		public String name;
		public String type;

		public boolean fulfilled;
		public boolean running;
	}
}


package eu.gaiax.notarization.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;


/**
 *
 * @author Mike Prechtl
 */
public record PagedNotarizationRequestSummary(
		int pageCount,
		long requestCount,
		List<NotarizationRequestSummary> notarizationRequests) {

	public static record NotarizationRequestSummary(
			UUID id,
			String profileId,
			OffsetDateTime createdAt,
			OffsetDateTime lastModified,
			String requestState,
			JsonNode data,
			String holder,
			int totalDocuments,
			String rejectComment) { }

}

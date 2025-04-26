/*
 *
 */
package eu.gaiax.notarization.environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.domain.DocumentStoreDocument;
import eu.gaiax.notarization.domain.DocumentUpload;
import eu.gaiax.notarization.domain.RequestSession;
import static eu.gaiax.notarization.domain.RequestSession.SessionTaskSummary;
import io.quarkus.panache.common.Parameters;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class DocumentManagement {

    public static final String SESSION_VARIABLE = "sessionId";
    public static final String DOCUMENTS_PATH = "/api/v1/session/{sessionId}/submission/documents";
    public static final String START_TASK_PATH = "/api/v1/session/{sessionId}/task";

    @Inject
    HttpResponseManagement httpManagement;

    @Inject
    Configuration configuration;

    private DocumentUpload latestUploadedDoc;

    public void uploadValidXMLDocument(RequestSession session) {
        uploadDocument(exampleSignedXMLDoc(), "application/xml", "xml", session);
    }

    public void uploadInvalidXMLDocument(RequestSession session) {
        uploadDocument(exampleInvalidXMLDoc(), "application/xml", "xml", session);
    }

    public void uploadValidPDFDocument(RequestSession session) {
        uploadDocument(exampleSignedPDFDoc(), "application/pdf", "pdf", session);
    }

    public DocumentUpload latestUploadedDoc() {
        return latestUploadedDoc;
    }

    public Optional<String> getVerificationReport(UUID id) {
        Optional<DocumentStoreDocument> storedDoc = DocumentStoreDocument
                .find("id = :id", Parameters.with("id", id))
                .firstResultOptional();
        return storedDoc.map(doc -> doc.verificationReport);
    }

    private void uploadDocument(Path contentPath, String mimetype, String extension, RequestSession session) {
        latestUploadedDoc = new DocumentUpload();
        latestUploadedDoc.id = UUID.randomUUID();
        latestUploadedDoc.content = contentPath;
        latestUploadedDoc.title = "directly uploaded doc";
        latestUploadedDoc.shortDescription = "short";
        latestUploadedDoc.longDescription = "long";
        latestUploadedDoc.mimetype = mimetype;
        latestUploadedDoc.extension = extension;

        uploadDocument(session, latestUploadedDoc);
    }

    private void uploadDocument(RequestSession session, DocumentUpload doc) {
        var accessToken = session.token();
        var location = session.location();
        var sessionId = session.sessionId();

        var task = receiveDocUploadTaskSummary(accessToken, location);
        var taskId = task.taskId.toString();

        startTask(sessionId, accessToken, taskId);
        uploadDocument(doc, sessionId, accessToken, taskId);
        finishTask(sessionId, accessToken, taskId);
    }

    private void startTask(String sessionId, String accessToken, String taskId) {
        given()
            .contentType(ContentType.JSON)
            .pathParam(SESSION_VARIABLE, sessionId)
            .header("token", accessToken)
            .queryParam("taskId", taskId)
            .when()
            .post(configuration.notarization().url().toString() + START_TASK_PATH)
            .then()
            .statusCode(201);
    }

    private void uploadDocument(DocumentUpload doc, String sessionId, String accessToken, String taskId) {
        var rawResponse = given()
            .pathParam(SESSION_VARIABLE, sessionId)
            .pathParam("taskId", taskId)
            .header("token", accessToken)
            .multiPart("content", doc.content.toFile(), doc.mimetype)
            .multiPart("id", doc.id.toString())
            .multiPart("title", doc.title)
            .multiPart("shortDescription", doc.shortDescription)
            .multiPart("longDescription", doc.longDescription)
            .contentType(ContentType.MULTIPART)
            .when()
            .post(configuration.notarization().url().toString() + "/api/v1/document/{sessionId}/{taskId}/upload")
            .then()
            .statusCode(204);

        httpManagement.lastResponse = rawResponse;
    }

    private void finishTask(String sessionId, String accessToken, String taskId) {
        given()
            .pathParam("taskId", taskId)
            .pathParam(SESSION_VARIABLE, sessionId)
            .header("token", accessToken)
            .when()
            .post(configuration.notarization().url().toString() + "/api/v1/document/{sessionId}/{taskId}/finishTask")
            .then()
            .statusCode(204);
    }

    private SessionTaskSummary receiveDocUploadTaskSummary(String accessToken, String location) {
        var tasks = given()
            .accept(ContentType.JSON)
            .header("token", accessToken)
            .when()
            .get(location)
            .then()
            .statusCode(200)
            .extract()
            .path("tasks");

        var objectMapper = new ObjectMapper();
        var tasklist = Arrays.asList(objectMapper.convertValue(tasks, SessionTaskSummary[].class ));

        return tasklist.stream()
                .filter(task -> task.name.equals("DocumentUpload"))
                .findAny().orElseThrow();
    }

    private Path exampleSignedPDFDoc() {
        String filePath = DocumentManagement.class.getResource("/docs/valid-signed-document.pdf").getPath();
        return Path.of(filePath);
    }

    private Path exampleSignedXMLDoc() {
        String filePath = DocumentManagement.class.getResource("/docs/valid-signed-document.xml").getPath();
        return Path.of(filePath);
    }

    private Path exampleInvalidXMLDoc() {
        String filePath = DocumentManagement.class.getResource("/docs/invalid-xades-structure.xml").getPath();
        return Path.of(filePath);
    }

}

/*
 *
 */
package eu.gaiax.notarization;

import eu.gaiax.notarization.environment.AuditingManagement;
import eu.gaiax.notarization.environment.DocumentManagement;
import eu.gaiax.notarization.environment.RequestManagement;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;


/**
 *
 * @author Mike Prechtl
 */
public class DatabaseStepDefinitions {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseStepDefinitions.class);

    @Inject
    AuditingManagement auditManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    DocumentManagement docManagement;

    @When("we view the auditing entries")
    public void we_view_the_auditing_entries() {
        auditManagement.fetchAuditEntries();
    }

    @Then("there should be a requestor audit entry for {string} with the action {string} after {int} seconds")
    public void there_should_be_an_entry_for_the_requestor_with_the_action(String requestName, String action, int seconds) throws InterruptedException{
        var secondsToWait = seconds;
        var sessionId = requestManagement.request(requestName).sessionId();
        var requestorEntryExists = false;
        do{
            Thread.sleep(seconds * 1000);
            requestorEntryExists = auditManagement.doesRequestorEntryExist(sessionId, action);
        } while(requestorEntryExists == false && secondsToWait-- >0);

        Assertions.assertTrue(requestorEntryExists);
    }

    @Then("there should be an operator audit entry for {string} with the action {string} after {int} seconds")
    public void there_should_be_an_entry_for_the_operator_with_the_action(String requestName, String action, int seconds) throws InterruptedException {
        var secondsToWait = seconds;
        var notarizationRequestId = requestManagement.requestId(requestName);
        var notaryEntryExists = false;
        do {
            Thread.sleep(seconds * 1000);
            notaryEntryExists = auditManagement.doesNotaryAuditEntryExist(notarizationRequestId, action);
        } while (notaryEntryExists == false && secondsToWait-- > 0);

        Assertions.assertTrue(notaryEntryExists);
    }

    @Then("the database contains a verification report that the signature was valid proven")
    public void the_database_contains_a_verification_report_that_the_signature_was_valid_proven() {
        var docId = docManagement.latestUploadedDoc().id;
        var verificationReportRaw = docManagement.getVerificationReport(docId);

        Assertions.assertTrue(verificationReportRaw.isPresent());
        var decodedVerificationReportBytes = Base64.getUrlDecoder().decode(verificationReportRaw.get());

        LOG.info(new String(decodedVerificationReportBytes));

        var mainIndication = getValueByXPath(
                "ValidationReport/SignatureValidationReport/SignatureValidationStatus/MainIndication",
                decodedVerificationReportBytes);
        Assertions.assertEquals("urn:etsi:019102:mainindication:total-passed", mainIndication);
    }

    @Then("the database contains a verification report that the signature was not valid with the reason {string}")
    public void the_database_contains_a_verification_report_that_the_signature_was_not_valid(String expectedReason)
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        var docId = docManagement.latestUploadedDoc().id;
        var verificationReportRaw = docManagement.getVerificationReport(docId);

        Assertions.assertTrue(verificationReportRaw.isPresent());
        var decodedVerificationReportBytes = Base64.getUrlDecoder().decode(verificationReportRaw.get());

        LOG.info(new String(decodedVerificationReportBytes));

        var mainIndication = getValueByXPath(
                "ValidationReport/SignatureValidationReport/SignatureValidationStatus/MainIndication",
                decodedVerificationReportBytes);
        var subindication = getValueByXPath(
                "ValidationReport/SignatureValidationReport/SignatureValidationStatus/SubIndication",
                decodedVerificationReportBytes);

        Assertions.assertEquals("urn:etsi:019102:mainindication:total-failed", mainIndication);
        Assertions.assertEquals(expectedReason, subindication);
    }

    private String getValueByXPath(String expression, byte[] docBytes) {
        try {
            var byteIn = new ByteArrayInputStream(docBytes);

            var builderFactory = DocumentBuilderFactory.newInstance();
            var builder = builderFactory.newDocumentBuilder();
            var xmlDocument = builder.parse(byteIn);

            var xPath = XPathFactory.newInstance().newXPath();
            return (String) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.STRING);
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException ex) {
            throw new IllegalStateException("Unable to extract value with the expression: " + expression);
        }
    }

}

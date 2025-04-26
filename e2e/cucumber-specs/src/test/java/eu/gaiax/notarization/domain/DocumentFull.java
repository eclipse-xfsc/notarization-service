/*
 *
 */
package eu.gaiax.notarization.domain;

import java.util.UUID;


/**
 *
 * @author Neil Crossley
 */
public class DocumentFull {

    public UUID id;
    public String title;
    public String shortDescription;
    public String longDescription;
    public String mimetype;
    public String extension;

    public byte[] content;
    public String verificationReport;
    public String hash;

}

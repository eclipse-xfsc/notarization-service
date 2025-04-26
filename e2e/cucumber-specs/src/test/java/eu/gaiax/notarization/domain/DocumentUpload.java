/*
 *
 */
package eu.gaiax.notarization.domain;

import java.nio.file.Path;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;


/**
 *
 * @author Neil Crossley
 */
public class DocumentUpload {

    @NotNull
    public UUID id;
    public Path content;
    public String title;
    public String mimetype;
    public String extension;
    public String shortDescription;
    public String longDescription;
}

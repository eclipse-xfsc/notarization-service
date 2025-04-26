/*
 *
 */
package eu.gaiax.notarization.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


/**
 *
 * @author Florian Otto
 */
@Entity
public class DocumentStoreDocument extends PanacheEntityBase {

    @Id
    public UUID id;

    public byte[] content;

    public UUID taskId;

    @Convert(converter = StringConverter.class)
    public String title;
    @Convert(converter = StringConverter.class)
    public String shortDescription;
    @Convert(converter = StringConverter.class)
    public String longDescription;

    @Convert(converter = StringConverter.class)
    public String mimetype;
    @Convert(converter = StringConverter.class)
    public String extension;

    @Convert(converter = StringConverter.class)
    public String verificationReport;
    public String hash;

    @CreationTimestamp
    public OffsetDateTime createdAt;
    @UpdateTimestamp
    public OffsetDateTime lastModified;

}

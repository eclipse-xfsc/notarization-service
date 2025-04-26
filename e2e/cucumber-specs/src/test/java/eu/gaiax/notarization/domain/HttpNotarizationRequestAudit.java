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


/**
 *
 * @author Neil Crossley
 */
@Entity
public class HttpNotarizationRequestAudit extends PanacheEntityBase {

    @Id
    public UUID id;

    public String requestUri;

    public String sessionId;

    public String notarizationId;

    public String ipAddress;

    public String action;

    public int httpStatus;

    public String caller;

    @Convert(converter = StringConverter.class)
    public String requestContent;

    public OffsetDateTime receivedAt;

    @CreationTimestamp
    public OffsetDateTime createdAt;
}

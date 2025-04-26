package eu.gaiax.notarization.request_processing.domain.entity

import eu.gaiax.notarization.api.issuance.ApiVersion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "issuance_process")
class IssuanceProcess: PanacheEntityBase {

    @Id
    lateinit var id: String

    @Column(name = "ssi_name")
    @Enumerated(EnumType.STRING)
    var issuerVersion: ApiVersion? = null

    var successCBToken: String? = null

    var failCBToken: String? = null

    var successCBUri: String? = null

    var failCBUri: String? = null

    var ssiInvitationUrl: String? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @ManyToOne
    lateinit var session: Session

    @Basic
    @Column(insertable = false, updatable = false)
    lateinit var session_id: String

    companion object : PanacheCompanionBase<IssuanceProcess, String> {

    }
}

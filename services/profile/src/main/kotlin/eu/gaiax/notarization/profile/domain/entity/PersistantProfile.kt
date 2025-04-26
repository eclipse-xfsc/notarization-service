package eu.gaiax.notarization.profile.domain.entity

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import eu.gaiax.notarization.profile.domain.model.ProfleIdentifier
import io.quarkiverse.hibernate.types.json.JsonNodeStringType
import io.quarkiverse.hibernate.types.json.JsonTypes
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.Uni
import jakarta.persistence.*
import jakarta.persistence.Table
import org.hibernate.annotations.*
import java.time.Instant
import java.time.Period
import java.util.*

@Entity
@Table(name = "persistent_profile")
class PersistantProfile : PanacheEntityBase {

    @Id
    var id: String? = null

    @NaturalId
    @Column(unique = true, name = "profile_id")
    var profileId: String? = null

    @Enumerated(EnumType.STRING)
    var capability: AipVersion? = null
    @Enumerated(EnumType.STRING)
    var kind: CredentialKind? = null
    var name: String? = null
    var description: String? = null

    @Convert(converter = StringSetJacksonConverter::class)
    @Column(name = "notary_roles")
    var notaryRoles: Set<String>? = null
    var encryption: String? = null

    @Convert(converter = NotaryAccessesJacksonConverter::class)
    @Column(columnDefinition = JsonTypes.JSON)
    var notaries: List<NotaryAccess>? = null

    @Convert(converter = PeriodConverter::class)
    @Column(name = "valid_for")
    var validFor: Period? = null

    @Column(name = "is_revocable")
    var isRevocable: Boolean? = null

    @Type(value = JsonNodeStringType::class)
    @Column(columnDefinition = JsonTypes.JSON)
    var template: JsonNode? = null
    var documentTemplate: String? = null

    @Convert(converter = TaskDescriptionsConverter::class)
    @Column(columnDefinition = JsonTypes.JSON)
    var taskDescriptions: List<TaskDescription>? = null

    @Convert(converter = ProfileTaskTreeJacksonConverter::class)
    @Column(columnDefinition = JsonTypes.JSON)
    var tasks: ProfileTaskTree? = null

    @Convert(converter = ProfileTaskTreeJacksonConverter::class)
    @Column(name = "precondition_tasks", columnDefinition = JsonTypes.JSON)
    var preconditionTasks: ProfileTaskTree? = null

    @Convert(converter = ProfileTaskTreeJacksonConverter::class)
    @Column(name = "pre_issuance_actions",
        columnDefinition = JsonTypes.JSON)
    var preIssuanceActions: ProfileTaskTree? = null

    @Convert(converter = StringsJacksonConverter::class)
    @Column(name = "post_issuance_actions")
    var postIssuanceActions: List<String>? = null

    @Convert(converter = IssuanceActionsConverter::class)
    @Column(name = "action_descriptions",
        columnDefinition = JsonTypes.JSON)
    var actionDescriptions: List<IssuanceAction>? = null

    @Convert(converter = ExtensionsConverter::class)
    @Column(name = "extensions",
        columnDefinition = JsonTypes.JSON
    )
    var extensions: Map<String, JsonNode>? = null

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at")
    lateinit var createdAt: Instant

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at")
    var updatedAt: Instant? = null

    companion object : PanacheCompanionBase<PersistantProfile, UUID> {
        @WithTransaction
        fun findByProfileId(profileId: String): Uni<PersistantProfile> {
            return PersistantProfile.find("profileId", profileId).singleResult()
                .onFailure(NoResultException::class.java)
                .transform { t -> UnknownProfileException(profileId, t) }
        }
        @WithTransaction
        fun findByProfileIdOptionally(profileId: String): Uni<PersistantProfile?> {
            return PersistantProfile.find("profileId", profileId).firstResult()
        }

        @WithTransaction
        fun allIdentifiers(): Uni<List<ProfleIdentifier>> {
            return PersistantProfile.findAll().project(ProfleIdentifier::class.java).list()
        }
    }
}

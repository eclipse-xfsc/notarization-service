package eu.gaiax.notarization.profile.domain.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.gaiax.notarization.api.profile.IssuanceAction
import eu.gaiax.notarization.api.profile.NotaryAccess
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.api.profile.TaskDescription
import eu.gaiax.notarization.profile.infrastructure.rest.serialization.EnhanceSerializationCustomizer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Period

val objectMapper = jacksonObjectMapper()

@Converter
@ApplicationScoped
open class TaskDescriptionsConverter : AttributeConverter<List<TaskDescription>, String> {

    companion object {
        val refType: JavaType = objectMapper.constructType(object : TypeReference<List<TaskDescription>>() {})
    }

    override fun convertToDatabaseColumn(attribute: List<TaskDescription>?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<TaskDescription>? {
        if ( dbData == null ) {
            return null;
        }
        return objectMapper.readValue(dbData, refType)
    }
}

@Converter
@ApplicationScoped
open class IssuanceActionsConverter : AttributeConverter<List<IssuanceAction>, String> {

    companion object {
        val refType: JavaType = objectMapper.constructType(object : TypeReference<List<IssuanceAction>>() {})
    }

    override fun convertToDatabaseColumn(attribute: List<IssuanceAction>?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<IssuanceAction>? {
        if ( dbData == null ) {
            return null;
        }
        return objectMapper.readValue(dbData, refType)
    }
}

@Converter
@ApplicationScoped
open class ExtensionsConverter : AttributeConverter<Map<String, JsonNode>, String> {
    override fun convertToDatabaseColumn(attribute: Map<String, JsonNode>?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, JsonNode>? {
        return dbData?.let { objectMapper.readValue(dbData) }
    }
}

@Converter
@ApplicationScoped
open class NotaryAccessesJacksonConverter : AttributeConverter<List<NotaryAccess>, String> {

    companion object {
        val refType: JavaType = objectMapper.constructType(object : TypeReference<List<NotaryAccess>>() {})

        init {
            val customizer = EnhanceSerializationCustomizer()
            customizer.customize(objectMapper)
        }
    }

    override fun convertToDatabaseColumn(attribute: List<NotaryAccess>?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<NotaryAccess>? {
        if ( dbData == null ) {
            return null
        }
        return objectMapper.readValue(dbData, refType)
    }
}

@Converter()
@ApplicationScoped
open class StringsJacksonConverter : AttributeConverter<List<String>, String> {

    companion object {
        val typeRef = object: TypeReference<List<String>>(){}
    }

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        if ( dbData == null ) {
            return null;
        }
        return objectMapper.readValue(dbData,  typeRef)
    }
}

@Converter()
@ApplicationScoped
class StringSetJacksonConverter : AttributeConverter<Set<String>, String> {

    companion object {
        val typeRef = object: TypeReference<Set<String>>(){}
    }

    override fun convertToDatabaseColumn(attribute: Set<String>?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): Set<String>? {
        if ( dbData == null ) {
            return null;
        }
        return objectMapper.readValue(dbData,  typeRef)
    }
}

@Converter()
@ApplicationScoped
open class ProfileTaskTreeJacksonConverter : AttributeConverter<ProfileTaskTree, String> {

    companion object {
        val typeRef = object: TypeReference<ProfileTaskTree>(){}
    }

    override fun convertToDatabaseColumn(attribute: ProfileTaskTree?): String? {
        if ( attribute == null ) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): ProfileTaskTree? {
        if ( dbData == null ) {
            return null;
        }
        return objectMapper.readValue(dbData,  typeRef)
    }
}

@Converter
@ApplicationScoped
open class PeriodConverter : AttributeConverter<Period, String> {

    companion object {
        val refType: JavaType = objectMapper.constructType(object : TypeReference<Period>() {})
    }

    override fun convertToDatabaseColumn(attribute: Period?): String? {
        if ( attribute == null ) {
            return null;
        }
        return attribute.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Period? {
        if ( dbData == null ) {
            return null;
        }
        return Period.parse(dbData)
    }
}

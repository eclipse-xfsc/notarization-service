/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package eu.gaiax.notarization.api

import eu.gaiax.notarization.ListCredentialIssueService
import eu.gaiax.notarization.RevocationConfig
import eu.gaiax.notarization.api.revocation.*
import eu.gaiax.notarization.api.revocation.sl2021.CredentialStatus2021
import eu.gaiax.notarization.db.*
import eu.gaiax.notarization.util.db.dbBlockingResolved
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import java.time.Instant


private val logger = KotlinLogging.logger {}

class ListStatusImpl : ListStatus {

    @Inject
    lateinit var listRepo: ListsRepo

    override suspend fun getList(listName: String): String = dbBlockingResolved {
        val list = resolveListByName(listRepo, listName)
        list.listCredential ?: throw WebApplicationException(
            "ListCredential has not been created yet.", Response.Status.NOT_FOUND
        )
    }

}


class ListManagementImpl : ListManagement {

    @Inject
    lateinit var config: RevocationConfig

    @Inject
    lateinit var listRepo: ListsRepo

    @Inject
    lateinit var entryRepo: ListEntryRepo

    @Inject
    lateinit var listService: ListService

    @Inject
    lateinit var issuerService: ListCredentialIssueService

    override fun getLists(): List<ListMapping> {
        return listRepo.listAll().map {
            ListMapping(it.profileName, it.listName)
        }
    }

    override fun registerList(profileName: String, issueCredential: Boolean): String {
        logger.info { "Registering status list for profile '$profileName'." }
        // get list for requested profile, if it exists we return with conflict
        listRepo.findByProfileName(profileName)?.let {
            // at this point we know, that the profile is registered, answer with 409 Conflict
            logger.warn { "Profile $profileName already exists, skipping its creation." }
            throw WebApplicationException("Profile $profileName already exists.", Response.Status.CONFLICT)
        }

        try {
            logger.info { "Registering status list for profile '$profileName'." }
            val newList = listService.createList(profileName)

            if (issueCredential) {
                try {
                    logger.info { "Trying to issue new list credential for profile '$profileName'." }
                    issuerService.issueCredential(newList)
                } catch (ex: Exception) {
                    logger.error(ex) { "Failed to issue new ListCredential for profile ${newList.profileName}." }
                }
            }

            logger.info { "Successfully registered profile '$profileName'." }

            return newList.listName
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to register status list for profile '$profileName'." }
            throw WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)
        }
    }

    override fun issueCredentials() {
        for (newList in listRepo.listAll()) {
            issueCredentialInternal(newList, false)
        }
    }

    override fun issueCredential(profileName: String, force: Boolean) {
        val newList = resolveListByProfile(listRepo, profileName)
        issueCredentialInternal(newList, force)
    }

    private fun issueCredentialInternal(newList: Lists, force: Boolean) {
        logger.debug { "Checking if ListCredential issuance is needed for ${newList.profileName} ..." }
        if (force || newList.lastUpdate.isBefore(Instant.now().minus(config.minIssueInterval()))) {
            try {
                issuerService.issueCredential(newList)
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to issue new ListCredential for profile ${newList.profileName}." }
            }
        } else {
            logger.debug { "No ListCredential update needed for profile ${newList.profileName}, due to renewal interval." }
        }
    }

    override fun getListDefinition(profileName: String): ListDefinition {
        return resolveListByProfile(listRepo, profileName).let {
            ListDefinition(
                listName = it.listName,
                profileName = it.profileName,
                encodedList = it.encodedList,
                listCredential = it.listCredential,
                lastUpdate = it.lastUpdate,
            )
        }
    }

    override fun addStatusEntry(profileName: String): CredentialStatus {
        logger.debug { "Adding status entry to profile ${profileName}." }
        val listRef = resolveListByProfile(listRepo, profileName)
        val entryIndex = listService.addEntry(listRef)

        val listUrl = listService.createStatusUrl(listRef.listName)
        val index = entryIndex.toString()
        return CredentialStatus2021(
            index = index,
            listUrl = listUrl,
            statusId = "$listUrl#$index",
        ).also {
            logger.debug { "Successfully added entry to status list with profile ${profileName}." }
        }
    }

    override fun getStatusEntry(profileName: String, idx: Long): ListEntry {
        val listRef = resolveListByProfile(listRepo, profileName)
        return entryRepo.findByIndex(listRef, idx)?.toListEntry()
            ?: throw WebApplicationException("Requested status entry does not exist.", Response.Status.NOT_FOUND)
    }

    override fun revoke(profileName: String, idx: Long) {
        logger.debug { "Revoking status entry with index ${idx} of profile ${profileName}." }
        val listRef = resolveListByProfile(listRepo, profileName)
        listService.revokeEntry(listRef, idx)
            .also {
                logger.debug { "Successfully revoked entry in status list with index ${idx} of profile ${profileName}." }
            }
    }

    override fun getEncodedList(
        profileName: String,
    ): String {
        val listRef = resolveListByProfile(listRepo, profileName)
        return issuerService.calculateBitSet(listRef, updateEntries = false)
    }

}


private fun resolveListByName(listRepo: ListsRepo, listName: String): Lists {
    return listRepo.findByListName(listName)
        ?: throw WebApplicationException("Requested status list does not exist.", Response.Status.NOT_FOUND)
}

private fun resolveListByProfile(listRepo: ListsRepo, profileName: String): Lists {
    return listRepo.findByProfileName(profileName)
        ?: throw WebApplicationException("Requested status list does not exist.", Response.Status.NOT_FOUND)
}

fun ListEntryEntity.toListEntry(): ListEntry {
    return ListEntry(
        index = index,
        createdAt = createdAt,
        revoked = revoked,
        revokedAt = revokedAt,
        processed = processed,
    )
}

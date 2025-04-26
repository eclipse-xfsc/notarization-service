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

package eu.gaiax.notarization

import eu.gaiax.notarization.api.IssuanceController
import eu.gaiax.notarization.api.revocation.ListCredentialRequestData
import eu.gaiax.notarization.api.revocation.sl2021.ListCredentialSubject2021
import eu.gaiax.notarization.db.*
import eu.xfsc.not.vc.status.StatusBitSet
import eu.xfsc.not.vc.status.StatusList2021Util
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import java.util.*
import java.util.stream.Stream


private val logger = KotlinLogging.logger {}

@ApplicationScoped
class ListCredentialIssueService {

    @Inject
    lateinit var listsRepo: ListsRepo

    @Inject
    lateinit var listEntryRepo: ListEntryRepo

    @Inject
    lateinit var listService: ListService

    @Inject
    lateinit var slUtil: StatusList2021Util

    @Inject
    @RestClient
    lateinit var issuanceController: IssuanceController

    fun issueCredential(list: Lists) {
        var tmpList = list
        val updateStart = Instant.now()

        if (tmpList.listCredential != null) {
            // check if we really need an update and abort if it is unneeded
            if (! listEntryRepo.hasUnprocessedEntries(tmpList)) {
                logger.debug{ "No ListCredential update needed for profile ${tmpList.profileName}, due to lack of new entries." }
                return
            }
        }

        logger.info { "Starting to issue new ListCredential for profile ${tmpList.profileName}." }

        val encoded = calculateBitSet(tmpList)
        // save encoded list, in case issuance fails
        tmpList = listService.updateEncodedList(tmpList, encoded)

        val listUrl = listService.createStatusUrl(tmpList.listName)
        val requestData = ListCredentialRequestData(
            listId = listUrl,
            subject = ListCredentialSubject2021(
                credentialId = "$listUrl#list",
                encodedList = encoded,
            ),
        )

        val credential = issuanceController.issueListCredential(tmpList.profileName, requestData)

        tmpList = listService.updateListCredential(tmpList, credential, updateStart)
        logger.info { "Finished issuing new ListCredential for profile ${tmpList.profileName}." }
    }

    @Transactional
    fun calculateBitSet(list: Lists, updateEntries: Boolean = true): String {
        val oldBitSet = getOldBitset(list)
        val changedEntries = listEntryRepo.findUnprocessedEntries(list)
        val curIdx = listService.showNextIndex(list)
        val numBytes = slUtil.determineBitstringNumBytes(curIdx)
        val newBitSet = buildBitSet(oldBitSet, changedEntries, updateEntries)
        return newBitSet.encode(numBytes.toInt())
    }

    fun getOldBitset(list: Lists): StatusBitSet {
        with (list.encodedList) {
            if (this == null) {
                return StatusBitSet(BitSet())
            } else {
                return StatusBitSet.decodeBitset(this)
            }
        }
    }

    private fun buildBitSet(bitset: StatusBitSet, changedEntries: Stream<ListEntryEntity>, updateEntries: Boolean): StatusBitSet {
        // update bitset
        for (nextEntry in changedEntries) {
            bitset.bitset.set(nextEntry.index.toInt())
            if (updateEntries) {
                nextEntry.processed = true
            }
        }

        return bitset
    }

}


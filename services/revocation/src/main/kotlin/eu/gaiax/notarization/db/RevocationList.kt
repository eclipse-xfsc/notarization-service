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

package eu.gaiax.notarization.db

import eu.gaiax.notarization.RevocationConfig
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.*
import jakarta.transaction.Transactional
import jakarta.ws.rs.core.UriBuilder
import org.hibernate.annotations.CurrentTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.generator.EventType
import java.io.Serializable
import java.time.Instant
import java.util.*
import java.util.stream.Stream


@Entity(name = "lists")
class Lists {
    @Id
    @Column(name = "list_name")
    lateinit var listName: String

    @Column(name = "profile_name")
    lateinit var profileName: String

    @Column(name = "encoded_list")
    var encodedList: String? = null

    @Column(name = "list_credential")
    var listCredential: String? = null

    @Column(name = "last_update")
    @CurrentTimestamp(source = SourceType.VM, event = [EventType.INSERT])
    lateinit var lastUpdate: Instant
}

@ApplicationScoped
class ListsRepo : PanacheRepository<Lists> {
    //fun findByIndex(list: EntryCounter, idx: Long): ListEntry? = find("list = ?1 and index = ?2", list, idx).firstResult()
    fun findByListName(listName: String): Lists? {
        return find("listName", listName).firstResult()
    }

    fun findByProfileName(profileName: String): Lists? {
        return find("profileName", profileName).firstResult()
    }
}


@Entity(name = "entry_counter")
class EntryCounter : Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "list_name")
    lateinit var list: Lists

    @Column(name = "last_idx")
    var lastIdx: Long = -1
}

@ApplicationScoped
class EntryCounterRepo : PanacheRepository<EntryCounter> {

    fun showNextIndex(listRef: Lists): Long {
        return find("list", listRef).firstResult()!!.lastIdx + 1
    }

    fun obtainNextIndex(listRef: Lists): Long {
        val entry = find("list", listRef).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()!!
        val nextVal = entry.lastIdx + 1
        entry.lastIdx = nextVal
        return nextVal
    }
}


@Entity(name = "list_entry")
class ListEntryEntity : Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "list_name")
    lateinit var list: Lists

    @Id
    var index: Long = -1 // init to -1 as kotlin does not allow lateinit with primitive types

    @Column(name = "created_at")
    @CurrentTimestamp(source = SourceType.VM, event = [EventType.INSERT])
    lateinit var createdAt: Instant

    @Column(name = "revoked")
    var revoked: Boolean = false

    @Column(name = "revoked_at")
    lateinit var revokedAt: Instant

    @Column(name = "processed_to_list")
    var processed: Boolean = false
}

@ApplicationScoped
class ListEntryRepo : PanacheRepository<ListEntryEntity> {
    fun findByIndex(list: Lists, idx: Long): ListEntryEntity? = find("list = ?1 and index = ?2", list, idx).firstResult()

    fun findByList(list: Lists): Stream<ListEntryEntity> {
        return find("list", list).stream()
    }

    fun findUnprocessedEntries(list: Lists): Stream<ListEntryEntity> {
        return find("list = ?1 and revoked = true and processed = false", list).stream()
    }

    @Transactional
    fun hasUnprocessedEntries(list: Lists): Boolean {
        return findUnprocessedEntries(list).findFirst().isPresent
    }
}


@ApplicationScoped
class ListService {
    @Inject
    lateinit var config: RevocationConfig

    @Inject
    lateinit var listsRepo: ListsRepo

    @Inject
    lateinit var entryCounterRepo: EntryCounterRepo

    @Inject
    lateinit var entryRepo: ListEntryRepo

    @Transactional
    fun createList(profileName: String): Lists {
        // create list entry
        val listName = UUID.randomUUID().toString()
        val newList = Lists().apply {
            this.profileName = profileName
            this.listName = listName
        }
        listsRepo.persist(newList)

        // create index
        val counter = EntryCounter().apply {
            list = newList
        }
        entryCounterRepo.persist(counter)

        return newList
    }

    @Transactional
    fun addEntry(listRef: Lists): Long {
        // increment counter
        val nextIdx = entryCounterRepo.obtainNextIndex(listRef)

        // add entry
        val newEntry = ListEntryEntity().apply {
            list = listRef
            index = nextIdx
        }
        entryRepo.persist(newEntry)

        return nextIdx
    }

    @Transactional
    fun revokeEntry(listRef: Lists, index: Long) {
        val entry = entryRepo.findByIndex(listRef, index)
        entry?.apply {
            revoked = true
            revokedAt = Instant.now()
        }
    }

    @Transactional
    fun updateEncodedList(list: Lists, encodedList: String): Lists {
        // import entity into this transaction
        val mergedList = listsRepo.getEntityManager().merge(list)
        listsRepo.getEntityManager().refresh(mergedList)
        mergedList.encodedList = encodedList
        return mergedList
    }

    @Transactional
    fun updateListCredential(list: Lists, credential: String, updateStart: Instant): Lists {
        // import entity into this transaction
        val mergedList = listsRepo.getEntityManager().merge(list)
        listsRepo.getEntityManager().refresh(mergedList)
        mergedList.lastUpdate = updateStart
        mergedList.listCredential = credential
        return mergedList
    }

    fun createStatusUrl(listName: String): String {
        val baseUrl = config.baseUrl()
        return UriBuilder.fromUri(baseUrl)
            .path("status").path(listName)
            .build().toString()
    }

    fun showNextIndex(list: Lists): Long {
        return entryCounterRepo.showNextIndex(list)
    }

}

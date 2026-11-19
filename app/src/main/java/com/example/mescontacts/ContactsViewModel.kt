package com.example.mescontacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * ViewModel partagé par les trois fragments (portée : MainActivity, via activityViewModels()).
 *
 * Choix de communication : le Détail enregistre directement dans ce ViewModel et la Liste
 * observe un LiveData. La liste est donc actualisée dès son retour à l'écran, sans passer
 * de résultat entre fragments. Le SavedStateHandle conserve les contacts et le filtre
 * en cas de rotation ou de destruction du processus.
 */
class ContactsViewModel(private val state: SavedStateHandle) : ViewModel() {

    private val contactsLive = state.getLiveData(KEY_CONTACTS, ArrayList(sampleContacts()))
    private val queryLive = state.getLiveData(KEY_QUERY, "")

    /** Liste affichée : tous les contacts filtrés par la recherche courante. */
    val contactsFiltres: LiveData<List<Contact>> = MediatorLiveData<List<Contact>>().apply {
        fun maj() {
            value = ContactFilter.filtrer(contactsLive.value.orEmpty(), queryLive.value.orEmpty())
        }
        addSource(contactsLive) { maj() }
        addSource(queryLive) { maj() }
    }

    val query: String get() = queryLive.value.orEmpty()

    fun setQuery(q: String) {
        if (q != queryLive.value) queryLive.value = q
    }

    /** Recherche par identifiant (jamais par position de ligne). */
    fun getContact(id: Long): Contact? = contactsLive.value?.firstOrNull { it.id == id }

    /** Ajoute ou remplace un contact. Retourne le contact enregistré (avec son id définitif). */
    fun enregistrer(contact: Contact): Contact {
        val liste = ArrayList(contactsLive.value.orEmpty())
        val index = liste.indexOfFirst { it.id == contact.id }
        val saved: Contact
        if (index >= 0) {
            val ancien = liste[index]
            if (ancien.photoPath != contact.photoPath) ImageUtils.deleteQuietly(ancien.photoPath)
            saved = contact
            liste[index] = saved
        } else {
            val nouvelId = (liste.maxOfOrNull { it.id } ?: 0L) + 1
            saved = contact.copy(id = nouvelId)
            liste.add(saved)
        }
        contactsLive.value = liste
        return saved
    }

    companion object {
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_QUERY = "query"
        const val NOUVEAU_CONTACT = -1L

        /** Contacts de démonstration : position valide ou aucune position. */
        fun sampleContacts() = listOf(
            Contact(1, "Dupont", "Claire", "06 12 34 56 78", "claire.dupont@example.com",
                latitude = 48.8584, longitude = 2.2945),          // Paris, tour Eiffel
            Contact(2, "Martin", "Thomas", "06 98 45 63 20", "thomas.martin@example.com",
                latitude = 36.8065, longitude = 10.1815),         // Tunis
            Contact(3, "Bernard", "Sophie", "06 60 71 24 32", "sophie.bernard@example.com",
                latitude = 35.8256, longitude = 10.6084),         // Sousse
            Contact(4, "Petit", "Lucas", "07 11 22 85 44", "lucas.petit@example.com",
                latitude = 34.7406, longitude = 10.7603),         // Sfax
            Contact(5, "Moreau", "Julien", "06 55 44 93 22", "julien.moreau@example.com")
            // Julien Moreau : aucune position (test du message « coordonnées absentes »)
        )
    }
}

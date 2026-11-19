package com.example.mescontacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFilterTest {

    private val contacts = listOf(
        Contact(1, "Dupont", "Claire", "06 12 34 56 78", ""),
        Contact(2, "Martin", "Thomas", "06 98 45 63 20", ""),
        Contact(3, "Lefèvre", "Hélène", "+216 98 765 432", "")
    )

    private fun ids(q: String) = ContactFilter.filtrer(contacts, q).map { it.id }

    @Test fun parNom() = assertEquals(listOf(1L), ids("dupont"))
    @Test fun parPrenom() = assertEquals(listOf(2L), ids("Thom"))
    @Test fun casseIgnoree() = assertEquals(listOf(1L), ids("DuPoNt"))
    @Test fun accentsIgnores() = assertEquals(listOf(3L), ids("helene"))
    @Test fun nomEtPrenom() = assertEquals(listOf(1L), ids("claire dupont"))
    @Test fun telephoneAvecEspaces() = assertEquals(listOf(1L), ids("06 12"))
    @Test fun telephoneSansEspaces() = assertEquals(listOf(2L), ids("069845"))
    @Test fun indicatif() = assertEquals(listOf(3L), ids("+216"))
    @Test fun aucunResultat() = assertTrue(ids("zzz").isEmpty())
    @Test fun filtreVide() = assertEquals(listOf(1L, 2L, 3L), ids(""))
    @Test fun filtreEspaces() = assertEquals(listOf(1L, 2L, 3L), ids("   "))
}

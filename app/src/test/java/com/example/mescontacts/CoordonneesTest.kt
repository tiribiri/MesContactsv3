package com.example.mescontacts

import com.example.mescontacts.Coordonnees.LAT_MAX
import com.example.mescontacts.Coordonnees.LAT_MIN
import com.example.mescontacts.Coordonnees.LNG_MAX
import com.example.mescontacts.Coordonnees.LNG_MIN
import com.example.mescontacts.Coordonnees.Resultat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordonneesTest {

    private fun lat(s: String) = Coordonnees.analyser(s, LAT_MIN, LAT_MAX)
    private fun lng(s: String) = Coordonnees.analyser(s, LNG_MIN, LNG_MAX)

    @Test fun latitudeValide() = assertEquals(Resultat.Ok(36.8065), lat("36.8065"))
    @Test fun virguleAcceptee() = assertEquals(Resultat.Ok(10.5), lng("10,5"))
    @Test fun negatif() = assertEquals(Resultat.Ok(-33.9), lat("-33.9"))
    @Test fun bornesIncluses() {
        assertEquals(Resultat.Ok(90.0), lat("90"))
        assertEquals(Resultat.Ok(-180.0), lng("-180"))
    }
    @Test fun vide() = assertEquals(Resultat.Vide, lat("  "))
    @Test fun nonNumerique() {
        assertEquals(Resultat.NonNumerique, lat("abc"))
        assertEquals(Resultat.NonNumerique, lat("1.2.3"))
        assertEquals(Resultat.NonNumerique, lat("-"))
    }
    @Test fun horsLimites() {
        assertEquals(Resultat.HorsLimites, lat("90.0001"))
        assertEquals(Resultat.HorsLimites, lat("-91"))
        assertEquals(Resultat.HorsLimites, lng("180.5"))
    }
    @Test fun contactSansPosition() =
        assertFalse(Contact(1, "A", "B", "1", "").aUnePosition)
    @Test fun contactAvecPosition() =
        assertTrue(Contact(1, "A", "B", "1", "", latitude = 36.8, longitude = 10.1).aUnePosition)
    @Test fun contactPositionIncomplete() =
        assertFalse(Contact(1, "A", "B", "1", "", latitude = 36.8).aUnePosition)
}

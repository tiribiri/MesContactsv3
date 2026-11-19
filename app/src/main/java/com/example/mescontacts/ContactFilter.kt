package com.example.mescontacts

import java.text.Normalizer
import java.util.Locale

/**
 * Stratégie de filtrage (fonction pure, testée dans ContactFilterTest) :
 * - porte sur le nom, le prénom, « nom prénom », « prénom nom » et le téléphone ;
 * - ignore la casse et les accents (« helene » trouve « Hélène ») ;
 * - pour le téléphone, les espaces, points et tirets sont ignorés (« 0612 » trouve « 06 12 … »).
 * Une recherche vide renvoie tous les contacts.
 */
object ContactFilter {

    fun filtrer(contacts: List<Contact>, query: String): List<Contact> {
        val q = normaliser(query)
        if (q.isEmpty()) return contacts
        val qChiffres = chiffres(query)
        return contacts.filter { c ->
            val nom = normaliser(c.nom)
            val prenom = normaliser(c.prenom)
            nom.contains(q) ||
                prenom.contains(q) ||
                "$nom $prenom".contains(q) ||
                "$prenom $nom".contains(q) ||
                normaliser(c.telephone).contains(q) ||
                (qChiffres.isNotEmpty() && chiffres(c.telephone).contains(qChiffres))
        }
    }

    /** Minuscules, sans accents, espaces multiples réduits. */
    fun normaliser(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")

    /** Ne garde que les chiffres et le « + » (indicatif international). */
    private fun chiffres(s: String): String =
        if (s.any { it.isLetter() }) "" else s.filter { it.isDigit() || it == '+' }
}

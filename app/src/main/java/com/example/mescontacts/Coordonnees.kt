package com.example.mescontacts

/** Analyse et validation des coordonnées GPS saisies à la main. */
object Coordonnees {

    const val LAT_MIN = -90.0
    const val LAT_MAX = 90.0
    const val LNG_MIN = -180.0
    const val LNG_MAX = 180.0

    /** Résultat de l'analyse d'un champ texte. */
    sealed class Resultat {
        object Vide : Resultat()
        object NonNumerique : Resultat()
        object HorsLimites : Resultat()
        data class Ok(val valeur: Double) : Resultat()
    }

    /** Accepte « 36.8 » comme « 36,8 ». */
    fun analyser(texte: String, min: Double, max: Double): Resultat {
        val t = texte.trim().replace(',', '.')
        if (t.isEmpty()) return Resultat.Vide
        val v = t.toDoubleOrNull()
        if (v == null || v.isNaN() || v.isInfinite()) return Resultat.NonNumerique
        if (v < min || v > max) return Resultat.HorsLimites
        return Resultat.Ok(v)
    }

    fun latitudeValide(v: Double?) = v != null && !v.isNaN() && v in LAT_MIN..LAT_MAX
    fun longitudeValide(v: Double?) = v != null && !v.isNaN() && v in LNG_MIN..LNG_MAX

    fun sontValides(lat: Double?, lng: Double?) = latitudeValide(lat) && longitudeValide(lng)
}

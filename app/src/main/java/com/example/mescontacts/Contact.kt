package com.example.mescontacts

import android.os.Parcel
import android.os.Parcelable

/**
 * Un contact : nom, prénom, téléphone, e-mail, photo et position GPS facultative.
 * Parcelable pour être sauvegardé dans le SavedStateHandle du ViewModel (mort du processus).
 * [photoPath] : chemin d'un JPEG dans le stockage interne (null = pas de photo).
 * [latitude] / [longitude] : null = aucune position définie.
 */
data class Contact(
    val id: Long,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable {

    val nomComplet: String get() = "$nom $prenom".trim()

    /** Vrai si le contact possède une position complète et dans les limites. */
    val aUnePosition: Boolean
        get() = Coordonnees.sontValides(latitude, longitude)

    private constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty(),
        parcel.readString(),
        readNullableDouble(parcel),
        readNullableDouble(parcel)
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(nom)
        dest.writeString(prenom)
        dest.writeString(telephone)
        dest.writeString(email)
        dest.writeString(photoPath)
        writeNullableDouble(dest, latitude)
        writeNullableDouble(dest, longitude)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact = Contact(parcel)
        override fun newArray(size: Int): Array<Contact?> = arrayOfNulls(size)

        private fun writeNullableDouble(dest: Parcel, value: Double?) {
            dest.writeInt(if (value == null) 0 else 1)
            if (value != null) dest.writeDouble(value)
        }

        private fun readNullableDouble(parcel: Parcel): Double? =
            if (parcel.readInt() == 1) parcel.readDouble() else null
    }
}

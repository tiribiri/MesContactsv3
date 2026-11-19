# Mes contacts – V3 (fragments, RecyclerView filtrable, Google Maps)

Application Android (Kotlin + XML) de gestion de contacts. La V3 reprend toutes les
fonctionnalités de la V2 et passe à une **activité unique + 3 fragments dynamiques**.

## Fonctionnalités
- Liste des contacts dans une **RecyclerView** (photo, nom, prénom, téléphone, icône si position GPS)
- **Recherche instantanée** (nom, prénom, téléphone ; insensible à la casse et aux accents)
- Fiche détaillée : modification, validation, Enregistrer / Annuler
- Appeler (ACTION_DIAL), envoyer un e-mail (ACTION_SENDTO), photo galerie / caméra
- **Latitude / longitude** affichées et modifiables (−90…90 / −180…180)
- **Voir sur la carte** : Google Maps centrée sur le contact avec un marqueur à son nom

## Architecture
```
MainActivity (FragmentContainerView)
 ├─ ListeContactsFragment   ── clic (id) ──►  DetailContactFragment  ── « Voir sur la carte » ──►  CarteContactFragment
 │        ▲                                          │                                                   (SupportMapFragment enfant)
 │        └──── observe LiveData ◄── ContactsViewModel (partagé, SavedStateHandle) ◄── enregistrer()
```
- Navigation : `replace()` + `addToBackStack()` ; le bouton Retour dépile la transaction.
- Paramètres : `Bundle` contenant l'**identifiant** du contact (jamais la position de la ligne).
- Actualisation : **ViewModel partagé** (`activityViewModels()`). La Liste observe un `LiveData`,
  le Détail écrit dans le ViewModel → la liste est à jour dès le retour, et les données survivent à la rotation.

## Clé Google Maps (non publiée)
1. Google Cloud Console → créer un projet → activer **Maps SDK for Android** → créer une clé API.
2. Restreindre la clé : *Applications Android*, package `com.example.mescontacts` + empreinte SHA-1
   (`./gradlew signingReport`), API limitée à *Maps SDK for Android*.
3. Ajouter dans **`local.properties`** (fichier ignoré par Git) :
   ```
   MAPS_API_KEY=VOTRE_CLE_ICI
   ```
4. `app/build.gradle.kts` lit cette valeur et l'injecte dans le manifeste via `manifestPlaceholders`.
   Sans clé, l'application fonctionne et l'écran Carte affiche un message explicite.

## Installation
1. Cloner le dépôt et l'ouvrir dans Android Studio
2. Ajouter `MAPS_API_KEY` dans `local.properties`
3. Lancer sur un émulateur **avec Google Play** ou un appareil réel

## Tests unitaires
`./gradlew test` – `ContactFilterTest` (filtrage) et `CoordonneesTest` (validation GPS).

## Auteur
Triki Mohamed Amine – L3MDW2

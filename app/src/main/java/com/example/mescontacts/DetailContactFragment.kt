package com.example.mescontacts

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mescontacts.databinding.FragmentDetailContactBinding
import java.io.File

/**
 * Écran Détail : consultation, modification, actions (appel, e-mail, galerie, caméra)
 * et accès à la carte. Reçoit uniquement l'identifiant du contact dans son Bundle.
 */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()

    private var contactId = ContactsViewModel.NOUVEAU_CONTACT
    private lateinit var original: Contact

    /**
     * Vrai une fois les champs remplis depuis le modèle. Ce champ vit dans l'instance du
     * fragment : au retour de la Carte (vue recréée), on ne réécrase pas les saisies en cours.
     */
    private var champsInitialises = false

    /** Photo affichée (non enregistrée tant qu'on n'a pas appuyé sur « Enregistrer »). */
    private var currentPhotoPath: String? = null

    /** Fichier temporaire donné à l'application caméra. */
    private var cameraTempFile: File? = null

    /** Photos créées pendant l'édition : supprimées si on annule. */
    private val newPhotos = ArrayList<String>()

    // --- Galerie -----------------------------------------------------------------------------
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) toast(R.string.msg_photo_selection_cancelled) else applyNewPhoto(uri)
        }

    // --- Caméra ------------------------------------------------------------------------------
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val temp = cameraTempFile
            cameraTempFile = null
            if (success && temp != null && temp.exists() && temp.length() > 0) {
                applyNewPhoto(Uri.fromFile(temp))
            } else {
                toast(R.string.msg_photo_capture_cancelled)
            }
            temp?.delete()
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else toast(R.string.msg_camera_permission_denied)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_CONTACT_ID, ContactsViewModel.NOUVEAU_CONTACT)
        if (savedInstanceState != null) {
            champsInitialises = true // les champs texte sont restaurés par le système
            currentPhotoPath = savedInstanceState.getString(STATE_PHOTO)
            cameraTempFile = savedInstanceState.getString(STATE_TEMP)?.let { File(it) }
            savedInstanceState.getStringArrayList(STATE_NEW_PHOTOS)?.let { newPhotos.addAll(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contact = if (contactId == ContactsViewModel.NOUVEAU_CONTACT) {
            Contact(ContactsViewModel.NOUVEAU_CONTACT, "", "", "", "")
        } else {
            viewModel.getContact(contactId)
        }
        if (contact == null) {
            toast(R.string.msg_invalid_contact)
            (requireActivity() as MainActivity).retour()
            return
        }
        original = contact

        binding.toolbar.setTitle(
            if (contactId == ContactsViewModel.NOUVEAU_CONTACT) R.string.title_new else R.string.title_detail
        )

        if (!champsInitialises) {
            binding.editLastName.setText(contact.nom)
            binding.editFirstName.setText(contact.prenom)
            binding.editPhone.setText(contact.telephone)
            binding.editEmail.setText(contact.email)
            binding.editLatitude.setText(contact.latitude?.toString().orEmpty())
            binding.editLongitude.setText(contact.longitude?.toString().orEmpty())
            currentPhotoPath = contact.photoPath
            champsInitialises = true
        }
        showPhoto()
        majResumePosition()

        // Efface le message d'erreur dès que l'utilisateur corrige le champ
        binding.editLastName.doAfterTextChanged { binding.layoutLastName.error = null }
        binding.editFirstName.doAfterTextChanged { binding.layoutFirstName.error = null }
        binding.editPhone.doAfterTextChanged { binding.layoutPhone.error = null }
        binding.editEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.editLatitude.doAfterTextChanged { binding.layoutLatitude.error = null; majResumePosition() }
        binding.editLongitude.doAfterTextChanged { binding.layoutLongitude.error = null; majResumePosition() }

        binding.btnCall.setOnClickListener { call() }
        binding.btnEmail.setOnClickListener { sendEmail() }
        binding.btnPickPhoto.setOnClickListener { pickFromGallery() }
        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnMap.setOnClickListener { voirSurLaCarte() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { cancel() }
        binding.toolbar.setNavigationOnClickListener { cancel() }

        // Le bouton Retour système = Annuler (lié au cycle de vie de la vue)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = cancel()
            })
    }

    // ===== Coordonnées GPS ======================================================================

    /** Affiche « Latitude : … / Longitude : … » ou « Aucune position définie ». */
    private fun majResumePosition() {
        val lat = Coordonnees.analyser(binding.editLatitude.text.toString(), Coordonnees.LAT_MIN, Coordonnees.LAT_MAX)
        val lng = Coordonnees.analyser(binding.editLongitude.text.toString(), Coordonnees.LNG_MIN, Coordonnees.LNG_MAX)
        binding.txtPosition.text = when {
            lat is Coordonnees.Resultat.Ok && lng is Coordonnees.Resultat.Ok ->
                getString(R.string.position_summary, lat.valeur, lng.valeur)
            lat is Coordonnees.Resultat.Vide && lng is Coordonnees.Resultat.Vide ->
                getString(R.string.position_none)
            else -> getString(R.string.position_invalid)
        }
    }

    /**
     * Valide les deux champs. Retourne la paire (lat, lng), (null, null) si les deux sont vides,
     * ou null si la saisie est invalide (les erreurs sont affichées sur les champs).
     */
    private fun lireCoordonnees(): Pair<Double?, Double?>? {
        val lat = Coordonnees.analyser(binding.editLatitude.text.toString(), Coordonnees.LAT_MIN, Coordonnees.LAT_MAX)
        val lng = Coordonnees.analyser(binding.editLongitude.text.toString(), Coordonnees.LNG_MIN, Coordonnees.LNG_MAX)

        if (lat is Coordonnees.Resultat.Vide && lng is Coordonnees.Resultat.Vide) return Pair(null, null)

        var ok = true
        when (lat) {
            Coordonnees.Resultat.Vide -> { binding.layoutLatitude.error = getString(R.string.error_lat_missing); ok = false }
            Coordonnees.Resultat.NonNumerique -> { binding.layoutLatitude.error = getString(R.string.error_not_number); ok = false }
            Coordonnees.Resultat.HorsLimites -> { binding.layoutLatitude.error = getString(R.string.error_lat_range); ok = false }
            is Coordonnees.Resultat.Ok -> Unit
        }
        when (lng) {
            Coordonnees.Resultat.Vide -> { binding.layoutLongitude.error = getString(R.string.error_lng_missing); ok = false }
            Coordonnees.Resultat.NonNumerique -> { binding.layoutLongitude.error = getString(R.string.error_not_number); ok = false }
            Coordonnees.Resultat.HorsLimites -> { binding.layoutLongitude.error = getString(R.string.error_lng_range); ok = false }
            is Coordonnees.Resultat.Ok -> Unit
        }
        if (!ok) return null
        return Pair((lat as Coordonnees.Resultat.Ok).valeur, (lng as Coordonnees.Resultat.Ok).valeur)
    }

    private fun voirSurLaCarte() {
        // On montre la position actuellement saisie (même si elle n'est pas encore enregistrée).
        // Saisie invalide (non numérique, hors limites, incomplète) : erreurs sur les champs,
        // message explicite et on reste sur le Détail.
        val coords = lireCoordonnees()
        if (coords == null) {
            toast(R.string.msg_coords_invalid)
            return
        }
        val nom = "${binding.editLastName.text.toString().trim()} ${binding.editFirstName.text.toString().trim()}"
            .trim().ifEmpty { getString(R.string.title_new) }
        // Coordonnées absentes : la Carte affiche un message « aucune position définie »
        (requireActivity() as MainActivity).ouvrirCarte(contactId, nom, coords.first, coords.second)
    }

    // ===== Appeler / e-mail (Intents implicites) =================================================

    private fun call() {
        val phone = binding.editPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.layoutPhone.error = getString(R.string.error_phone_empty)
            return
        }
        // ACTION_DIAL : ouvre le composeur avec le numéro prérempli (sans permission CALL_PHONE)
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_dialer)
        }
    }

    private fun sendEmail() {
        val email = binding.editEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.layoutEmail.error = getString(R.string.error_email_empty)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            return
        }
        // ACTION_SENDTO + mailto: : seules les applications de messagerie répondent
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_mail_app)
        }
    }

    // ===== Photo : galerie et caméra ============================================================

    private fun pickFromGallery() {
        try {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_gallery)
        }
    }

    private fun takePhoto() {
        val ctx = requireContext()
        if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(R.string.msg_camera_unavailable)
            return
        }
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val temp = File(ImageUtils.photosDir(ctx), "tmp_${System.currentTimeMillis()}.jpg")
        cameraTempFile = temp
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", temp)
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraTempFile = null
            temp.delete()
            toast(R.string.msg_no_camera_app)
        } catch (e: SecurityException) {
            cameraTempFile = null
            temp.delete()
            toast(R.string.msg_camera_permission_denied)
        }
    }

    /** Copie l'image choisie/prise dans le stockage interne puis l'affiche. */
    private fun applyNewPhoto(source: Uri) {
        val ctx = context ?: return
        val dest = File(ImageUtils.photosDir(ctx), "photo_${System.nanoTime()}.jpg")
        if (!ImageUtils.importPhoto(ctx, source, dest)) {
            toast(R.string.msg_photo_load_failed)
            return
        }
        // Une photo non enregistrée remplacée par une autre est supprimée tout de suite
        currentPhotoPath?.let { previous ->
            if (newPhotos.remove(previous)) ImageUtils.deleteQuietly(previous)
        }
        newPhotos.add(dest.path)
        currentPhotoPath = dest.path
        if (_binding != null) showPhoto()
    }

    private fun showPhoto() {
        val bitmap = currentPhotoPath?.let { ImageUtils.decodeScaled(it, 512) }
        if (bitmap != null) {
            binding.imgPhoto.setImageBitmap(bitmap)
        } else {
            binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
        }
    }

    // ===== Enregistrer / Annuler ================================================================

    private fun save() {
        val nom = binding.editLastName.text.toString().trim()
        val prenom = binding.editFirstName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // Validation : on signale tous les champs invalides et on met le focus sur le premier
        var firstInvalid: View? = null
        if (nom.isEmpty()) {
            binding.layoutLastName.error = getString(R.string.error_last_name)
            firstInvalid = binding.editLastName
        }
        if (prenom.isEmpty()) {
            binding.layoutFirstName.error = getString(R.string.error_first_name)
            firstInvalid = firstInvalid ?: binding.editFirstName
        }
        if (phone.isEmpty()) {
            binding.layoutPhone.error = getString(R.string.error_phone_empty)
            firstInvalid = firstInvalid ?: binding.editPhone
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.layoutPhone.error = getString(R.string.error_phone_invalid)
            firstInvalid = firstInvalid ?: binding.editPhone
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            firstInvalid = firstInvalid ?: binding.editEmail
        }
        val coords = lireCoordonnees()
        if (coords == null) {
            firstInvalid = firstInvalid
                ?: if (binding.layoutLatitude.error != null) binding.editLatitude else binding.editLongitude
        }
        if (firstInvalid != null || coords == null) {
            firstInvalid?.requestFocus()
            return
        }

        val updated = original.copy(
            nom = nom,
            prenom = prenom,
            telephone = phone,
            email = email,
            photoPath = currentPhotoPath,
            latitude = coords.first,
            longitude = coords.second
        )

        // On garde uniquement la photo finale parmi celles créées pendant l'édition
        newPhotos.filter { it != currentPhotoPath }.forEach { ImageUtils.deleteQuietly(it) }
        newPhotos.clear()

        // Enregistrement dans le ViewModel partagé → la Liste observe et s'actualise
        viewModel.enregistrer(updated)
        toast(R.string.msg_saved)
        (requireActivity() as MainActivity).retour()
    }

    /** Retour à la liste sans enregistrer : les anciennes données restent inchangées. */
    private fun cancel() {
        newPhotos.forEach { ImageUtils.deleteQuietly(it) }
        newPhotos.clear()
        cameraTempFile?.delete()
        cameraTempFile = null
        (requireActivity() as MainActivity).retour()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PHOTO, currentPhotoPath)
        outState.putString(STATE_TEMP, cameraTempFile?.path)
        outState.putStringArrayList(STATE_NEW_PHOTOS, newPhotos)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toast(resId: Int) {
        context?.let { Toast.makeText(it, resId, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        const val TAG = "detail"
        private const val ARG_CONTACT_ID = "contact_id"
        private const val STATE_PHOTO = "state_photo"
        private const val STATE_TEMP = "state_temp"
        private const val STATE_NEW_PHOTOS = "state_new_photos"

        /** Seul l'identifiant est transmis (Bundle) : le contact est relu dans le ViewModel. */
        fun newInstance(contactId: Long) = DetailContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_CONTACT_ID, contactId) }
        }
    }
}

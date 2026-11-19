package com.example.mescontacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mescontacts.databinding.FragmentCarteContactBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Écran Carte (SDK Google Maps) : centre la carte sur la position du contact
 * et place un marqueur portant son nom.
 * Message explicite si la position est absente ou invalide, ou si la clé API n'est pas configurée.
 * Aucune permission de localisation : on affiche des coordonnées déjà enregistrées.
 */
class CarteContactFragment : Fragment() {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarteContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val nom = args.getString(ARG_NOM).orEmpty()
        val lat = if (args.containsKey(ARG_LAT)) args.getDouble(ARG_LAT) else null
        val lng = if (args.containsKey(ARG_LNG)) args.getDouble(ARG_LNG) else null

        binding.toolbar.title = getString(R.string.title_map, nom)
        // Retour vers le Détail (le bouton Retour système dépile aussi la transaction)
        binding.toolbar.setNavigationOnClickListener { (requireActivity() as MainActivity).retour() }

        when {
            lat == null && lng == null -> afficherMessage(getString(R.string.map_no_position, nom))
            !Coordonnees.sontValides(lat, lng) -> afficherMessage(getString(R.string.map_invalid_position))
            !BuildConfig.HAS_MAPS_KEY -> afficherMessage(getString(R.string.map_no_api_key))
            else -> afficherCarte(LatLng(lat!!, lng!!), nom)
        }
    }

    private fun afficherMessage(message: String) {
        binding.txtMapMessage.text = message
        binding.layoutMessage.visibility = View.VISIBLE
        binding.mapContainer.visibility = View.GONE
        binding.txtCoords.visibility = View.GONE
    }

    private fun afficherCarte(position: LatLng, nom: String) {
        binding.layoutMessage.visibility = View.GONE
        binding.mapContainer.visibility = View.VISIBLE
        binding.txtCoords.visibility = View.VISIBLE
        binding.txtCoords.text = getString(R.string.position_summary, position.latitude, position.longitude)

        // SupportMapFragment enfant : recréé automatiquement après rotation, on ne l'ajoute qu'une fois
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.mapContainer, it).commit()
            }

        // Le rappel est appelé quand la carte est prête
        mapFragment.getMapAsync { map ->
            map.uiSettings.isZoomControlsEnabled = true
            map.clear()
            // Marqueur portant le nom du contact + centrage de la caméra sur sa position
            map.addMarker(MarkerOptions().position(position).title(nom))?.showInfoWindow()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "carte"
        private const val ARG_ID = "contact_id"
        private const val ARG_NOM = "nom"
        private const val ARG_LAT = "latitude"
        private const val ARG_LNG = "longitude"
        private const val ZOOM = 14f

        fun newInstance(contactId: Long, nom: String, latitude: Double?, longitude: Double?) =
            CarteContactFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, contactId)
                    putString(ARG_NOM, nom)
                    latitude?.let { putDouble(ARG_LAT, it) }
                    longitude?.let { putDouble(ARG_LNG, it) }
                }
            }
    }
}

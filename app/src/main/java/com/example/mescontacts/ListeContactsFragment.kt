package com.example.mescontacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mescontacts.databinding.FragmentListeContactsBinding

/** Écran Liste : RecyclerView + champ de recherche filtrant au fil de la saisie. */
class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clic → on transmet l'identifiant du contact, pas la position de la ligne
        adapter = ContactAdapter { contact -> (requireActivity() as MainActivity).ouvrirDetail(contact.id) }
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        // Le filtre est conservé dans le ViewModel (retour depuis le Détail, rotation)
        if (binding.editSearch.text.toString() != viewModel.query) {
            binding.editSearch.setText(viewModel.query)
        }
        // Filtrage immédiat, sans bouton de validation
        binding.editSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewModel.contactsFiltres.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            afficherEtatVide(liste.isEmpty())
        }

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_add) {
                (requireActivity() as MainActivity).ouvrirDetail(ContactsViewModel.NOUVEAU_CONTACT)
                true
            } else false
        }
    }

    private fun afficherEtatVide(vide: Boolean) {
        binding.txtEmpty.visibility = if (vide) View.VISIBLE else View.GONE
        binding.recyclerContacts.visibility = if (vide) View.GONE else View.VISIBLE
        if (vide) {
            binding.txtEmpty.text = if (viewModel.query.isBlank()) {
                getString(R.string.empty_list)
            } else {
                getString(R.string.empty_search, viewModel.query.trim())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerContacts.adapter = null
        _binding = null
    }

    companion object {
        const val TAG = "liste"
    }
}

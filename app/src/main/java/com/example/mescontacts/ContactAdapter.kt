package com.example.mescontacts

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mescontacts.databinding.ItemContactBinding

/**
 * Adapter de la RecyclerView (ListAdapter + DiffUtil).
 * Le clic renvoie le Contact de la ligne (donc son id), jamais une position :
 * aucune erreur d'indice possible quand la liste est filtrée.
 */
class ContactAdapter(
    private val onClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    // Les chemins de photos sont uniques (nouveau fichier à chaque changement de photo)
    private val cache = LruCache<String, Bitmap>(40)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var contact: Contact? = null

        init {
            binding.root.setOnClickListener { contact?.let(onClick) }
        }

        fun bind(c: Contact) {
            contact = c
            binding.txtName.text = c.nomComplet
            binding.txtPhone.text = c.telephone
            binding.imgPin.visibility =
                if (c.aUnePosition) android.view.View.VISIBLE else android.view.View.GONE

            val bitmap = c.photoPath?.let { path ->
                cache.get(path) ?: ImageUtils.decodeScaled(path, 160)?.also { cache.put(path, it) }
            }
            if (bitmap != null) {
                binding.imgPhoto.setImageBitmap(bitmap)
            } else {
                binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(old: Contact, new: Contact) = old.id == new.id
            override fun areContentsTheSame(old: Contact, new: Contact) = old == new
        }
    }
}

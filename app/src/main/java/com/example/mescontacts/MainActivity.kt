package com.example.mescontacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.mescontacts.databinding.ActivityMainBinding

/**
 * Activité unique : elle héberge les fragments dans un FragmentContainerView
 * et centralise la navigation (replace + addToBackStack).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Après une rotation, le FragmentManager recrée lui-même les fragments et la pile :
        // on n'ajoute la Liste qu'au premier lancement pour ne pas l'empiler en double.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainer, ListeContactsFragment(), ListeContactsFragment.TAG)
            }
        }
    }

    fun ouvrirDetail(contactId: Long) =
        naviguer(DetailContactFragment.newInstance(contactId), DetailContactFragment.TAG)

    fun ouvrirCarte(contactId: Long, nom: String, latitude: Double?, longitude: Double?) =
        naviguer(
            CarteContactFragment.newInstance(contactId, nom, latitude, longitude),
            CarteContactFragment.TAG
        )

    /** Retour à l'écran précédent de la pile. */
    fun retour() {
        if (!supportFragmentManager.isStateSaved) supportFragmentManager.popBackStack()
    }

    private fun naviguer(fragment: Fragment, tag: String) {
        // Évite d'empiler deux fois le même écran (double clic rapide)
        if (supportFragmentManager.isStateSaved) return
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.tag == tag) return
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(R.id.fragmentContainer, fragment, tag)
            addToBackStack(tag)
        }
    }
}

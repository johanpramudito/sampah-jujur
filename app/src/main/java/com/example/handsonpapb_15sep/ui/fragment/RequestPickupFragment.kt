package com.example.handsonpapb_15sep.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.handsonpapb_15sep.R

/**
 * Simplified Fragment for households to create pickup requests.
 * This version works without Firebase for testing purposes.
 */
class RequestPickupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request_pickup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show a welcome message for now
        Toast.makeText(requireContext(), "Welcome to Sampah Jujur - Request Pickup", Toast.LENGTH_SHORT).show()

        // TODO: Implement full functionality with Firebase
        // setupUI()
        // setupObservers()
        // setupLocationServices()
    }
}

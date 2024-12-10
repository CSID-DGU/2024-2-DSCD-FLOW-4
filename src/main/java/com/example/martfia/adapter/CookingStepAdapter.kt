package com.example.martfia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.martfia.R

class CookingStepAdapter(private val steps: Map<String, String>) :
    RecyclerView.Adapter<CookingStepAdapter.ViewHolder>() {

    private val stepsList = steps.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumberTextView: TextView = view.findViewById(R.id.stepNumberTextView)
        val stepDescriptionTextView: TextView = view.findViewById(R.id.stepDescriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_method, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (stepNumber, description) = stepsList[position]
        holder.stepNumberTextView.text = stepNumber
        holder.stepDescriptionTextView.text = description
    }

    override fun getItemCount(): Int = stepsList.size
}

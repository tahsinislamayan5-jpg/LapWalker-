package com.takwa.lapwalker.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.takwa.lapwalker.R
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity

class BadgeAdapter(
    private val badges: List<PerformanceBadgeEntity>
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tv_badge_icon)
        val tvName: TextView = view.findViewById(R.id.tv_badge_name)
        val tvDesc: TextView = view.findViewById(R.id.tv_badge_desc)
        val tvStatus: TextView = view.findViewById(R.id.tv_badge_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        holder.tvName.text = badge.name
        holder.tvDesc.text = badge.description

        if (badge.isUnlocked) {
            holder.tvIcon.text = "🎖️"
            holder.tvIcon.alpha = 1.0f
            holder.tvStatus.text = "EARNED"
            holder.tvStatus.setTextColor(0xFF10B981.toInt())
            holder.tvName.setTextColor(0xFF0F172A.toInt())
        } else {
            holder.tvIcon.text = "🔒"
            holder.tvIcon.alpha = 0.4f
            holder.tvStatus.text = "LOCKED"
            holder.tvStatus.setTextColor(0xFF94A3B8.toInt())
            holder.tvName.setTextColor(0xFF64748B.toInt())
        }
    }

    override fun getItemCount(): Int = badges.size
}

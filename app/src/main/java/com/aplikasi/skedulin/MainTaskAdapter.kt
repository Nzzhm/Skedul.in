package com.aplikasi.skedulin

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainTaskAdapter(
    private val tasks: List<Tugas>,
    private val onItemClick: (Tugas) -> Unit
) : RecyclerView.Adapter<MainTaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTaskName: TextView = itemView.findViewById(R.id.text_task_name)
        val textTaskDeadline: TextView = itemView.findViewById(R.id.text_task_deadline)
        val textTaskDate: TextView = itemView.findViewById(R.id.text_task_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val context = holder.itemView.context

        // Set task name
        holder.textTaskName.text = task.namatugas

        // Handle completed tasks
        if (task.selesai) {
            holder.textTaskName.paintFlags = holder.textTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.textTaskName.alpha = 0.6f
        } else {
            holder.textTaskName.paintFlags = holder.textTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.textTaskName.alpha = 1.0f
        }

        // FIX: Set deadline dengan jam menggunakan dua TextView sesuai layout
        if (task.deadline != null && task.deadline!! > 0) {
            val currentTime = System.currentTimeMillis()
            val deadlineTime = task.deadline!!

            // Format tanggal dan jam terpisah
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val deadlineDate = dateFormat.format(Date(deadlineTime))
            val deadlineTimeStr = timeFormat.format(Date(deadlineTime))

            when {
                deadlineTime < currentTime && !task.selesai -> {
                    // Terlambat
                    holder.textTaskDeadline.text = "⚠️ Terlambat"
                    holder.textTaskDate.text = "$deadlineDate, $deadlineTimeStr"
                    holder.textTaskDeadline.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    holder.textTaskDate.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
                deadlineTime - currentTime <= 24 * 60 * 60 * 1000 && !task.selesai -> {
                    // Deadline dalam 24 jam
                    holder.textTaskDeadline.text = "🔥 Deadline"
                    holder.textTaskDate.text = "$deadlineDate, $deadlineTimeStr"
                    holder.textTaskDeadline.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    holder.textTaskDate.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                }
                else -> {
                    // Normal
                    holder.textTaskDeadline.text = "📅 Deadline"
                    holder.textTaskDate.text = "$deadlineDate, $deadlineTimeStr"
                    holder.textTaskDeadline.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    holder.textTaskDate.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
            }

            // Tampilkan kedua TextView
            holder.textTaskDeadline.visibility = View.VISIBLE
            holder.textTaskDate.visibility = View.VISIBLE
        } else {
            // Tidak ada deadline
            holder.textTaskDeadline.text = "📝 Tugas"
            holder.textTaskDate.text = "Tidak ada deadline"
            holder.textTaskDeadline.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            holder.textTaskDate.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            holder.textTaskDeadline.visibility = View.VISIBLE
            holder.textTaskDate.visibility = View.VISIBLE
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onItemClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size
}
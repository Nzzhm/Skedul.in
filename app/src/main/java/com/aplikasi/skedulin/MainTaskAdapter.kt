package com.aplikasi.skedulin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainTaskAdapter(
    private val tasks: List<Tugas>,
    private val onTaskClick: (Tugas) -> Unit
) : RecyclerView.Adapter<MainTaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_task_item)
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

        holder.textTaskName.text = task.namatugas

        // Format deadline
        if (task.deadline != null) {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            holder.textTaskDate.text = sdf.format(Date(task.deadline))

            // Check if overdue
            val isOverdue = task.deadline < System.currentTimeMillis()
            if (isOverdue) {
                holder.textTaskDeadline.text = "Deadline"
                holder.textTaskDeadline.setTextColor(Color.parseColor("#F44336"))
                holder.textTaskDate.setTextColor(Color.parseColor("#F44336"))
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            } else {
                holder.textTaskDeadline.text = "Deadline"
                holder.textTaskDeadline.setTextColor(Color.parseColor("#666666"))
                holder.textTaskDate.setTextColor(Color.parseColor("#333333"))
                holder.cardView.setCardBackgroundColor(Color.WHITE)
            }
        } else {
            holder.textTaskDeadline.text = "Deadline"
            holder.textTaskDate.text = "Tidak ada deadline"
            holder.textTaskDeadline.setTextColor(Color.parseColor("#666666"))
            holder.textTaskDate.setTextColor(Color.parseColor("#999999"))
        }

        holder.cardView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size
}
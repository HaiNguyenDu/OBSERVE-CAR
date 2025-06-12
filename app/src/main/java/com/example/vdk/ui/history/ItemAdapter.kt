package com.example.vdk.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vdk.R
import com.example.vdk.databinding.ItemWarningBinding
import com.example.vdk.model.Tracking

class ItemAdapter(
    private val context: Context,
    private var list: MutableList<Tracking>,
) : RecyclerView.Adapter<ItemAdapter.ItemAdapterViewHolder>() {

    inner class ItemAdapterViewHolder(
        private val binding: ItemWarningBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun onHolder(tracking: Tracking) {
            binding.apply {
                tracking.timestamp?.let { timestampInSeconds ->
                    val currentTimeInSeconds = System.currentTimeMillis() / 1000
                    val differenceInSeconds = Math.abs(currentTimeInSeconds - timestampInSeconds)

                    val timeAgoString = when {
                        differenceInSeconds < 60 -> "${differenceInSeconds.toLong()} giây trước"
                        differenceInSeconds < 3600 -> "${(differenceInSeconds / 60).toLong()} phút trước"
                        else -> {
                            val hours = (differenceInSeconds / 3600).toLong()
                            val remainingMinutes = ((differenceInSeconds % 3600) / 60).toLong()
                            "$hours giờ $remainingMinutes phút trước"
                        }
                    }

                    tvTime.text = timeAgoString


                    if (tracking.state) {
                        tvTitle.text = "Phát hiện kẻ lạ"
                        tvDe.text = "Phát hiện có người lạ trong khu vực của bạn."
                        icCard.setBackgroundResource(R.drawable.bg_card)
                        tvWarning.text = "Cao"
                    } else { // state = false là an toàn
                        tvTitle.text = "Phát hiện chuyển động"
                        tvDe.text = "Phát hiện chuyển động an toàn trong khu vực."
                        icCard.setBackgroundResource(R.drawable.bg_green)
                        tvWarning.text = "An toàn"
                        tvWarning.setTextColor(context.getColor(R.color.blue))
                        Glide.with(context)
                            .load(R.drawable.ic_people)
                            .into(icWarning)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapterViewHolder {
        val binding = ItemWarningBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemAdapterViewHolder, position: Int) {
        holder.onHolder(list[position])
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Tracking>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}

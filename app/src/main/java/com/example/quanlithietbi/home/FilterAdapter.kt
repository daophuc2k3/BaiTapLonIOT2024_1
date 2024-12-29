package com.example.quanlithietbi.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlithietbi.R
import com.example.quanlithietbi.databinding.FieldFilterItemBinding

class FilterAdapter : RecyclerView.Adapter<FilterAdapter.FilterVH>() {

    interface IFilterCallback {
        fun onFilter(filter: String)
    }

    private val dataList = arrayListOf<FilterDisplay>()

    var callBack: IFilterCallback? = null

    fun setList(data: List<FilterDisplay>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    inner class FilterVH(private val binding: FieldFilterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvFilter.setOnClickListener {
                var oldIndex = dataList.indexOfFirst {
                    it.isSelect
                }
                if (oldIndex >= 0 && oldIndex != bindingAdapterPosition) {
                    dataList[oldIndex].isSelect = false
                    notifyItemChanged(oldIndex)
                    dataList[bindingAdapterPosition].isSelect = true
                    notifyItemChanged(bindingAdapterPosition)
                    callBack?.onFilter(dataList[bindingAdapterPosition].filter)
                }
            }
        }

        fun onBind(filter: FilterDisplay) {
            if (filter.isSelect) {
                binding.tvFilter.setTextColor(binding.root.resources.getColor(R.color.white))
                binding.tvFilter.background =
                    binding.root.resources.getDrawable(R.drawable.shape_bg_color_black_corner_8)
            } else {
                binding.tvFilter.setTextColor(binding.root.resources.getColor(R.color.black))
                binding.tvFilter.background =
                    binding.root.resources.getDrawable(R.drawable.shape_bg_colorwhite_corner_8)
            }
            binding.tvFilter.text = filter.filter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterVH {
        return FilterVH(
            FieldFilterItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: FilterVH, position: Int) {
        holder.onBind(dataList[position])
    }
}
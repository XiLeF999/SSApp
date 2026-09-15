package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.sansan.picture.R
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.show
import com.sansan.picture.utils.toChineseDate

class PictureAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val isKy1045 = context.resources.getBoolean(R.bool.is_ky1045)
    private var pictureList: List<Picture> = emptyList()
    private var displayItems: List<Item> = emptyList()
    private var isManager = false
    private var onItemClickListener: OnItemClickListener? = null
    private var onSelectItemListener: OnSelectItemListener? = null

    private sealed class Item {
        data class Header(val date: String, val count: Int) : Item()
        data class Photo(val picture: Picture) : Item()
    }

    fun setData(list: List<Picture>) {
        pictureList = list.sortedByDescending { it.dateTaken }
        displayItems = buildItems(pictureList)
        notifyDataSetChanged()
    }

    private fun buildItems(list: List<Picture>): List<Item> = buildList {
        list.groupBy { it.dateTaken.toChineseDate() }.forEach { (date, pictures) ->
            add(Item.Header(date, pictures.size))
            pictures.forEach { add(Item.Photo(it)) }
        }
    }

    fun setManager(manager: Boolean) {
        isManager = manager
        notifyDataSetChanged()
        notifySelectCount()
    }

    fun setAllSelect() {
        pictureList.forEach { it.isSelect = true }
        notifyDataSetChanged()
        notifySelectCount()
    }

    fun setCancelManager() {
        pictureList.forEach { it.isSelect = false }
        isManager = false
        notifyDataSetChanged()
        notifySelectCount()
    }

    override fun getItemViewType(position: Int): Int = when (displayItems[position]) {
        is Item.Header -> VIEW_TYPE_HEADER
        is Item.Photo -> VIEW_TYPE_PHOTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderHolder(inflater.inflate(context.resources.getIdentifier("layout_picture_date_header", "layout", context.packageName), parent, false) as TextView)
        } else {
            PhotoHolder(inflater.inflate(R.layout.layout_picture_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is Item.Header -> (holder as HeaderHolder).dateView.text = "${item.date}（${item.count}）"
            is Item.Photo -> bindPhoto(holder as PhotoHolder, item.picture)
        }
    }

    private fun bindPhoto(holder: PhotoHolder, picture: Picture) {
        Glide.with(context)
            .load(picture.uri)
            .transform(MultiTransformation(CenterCrop(), RoundedCorners(8)))
            .into(holder.pictureView)

        holder.nameView?.text = picture.name.takeIf { it.isNotBlank() }
            ?: picture.dateTaken.toChineseDate()
        holder.selectedOverlay?.visibility = if (isKy1045 && isManager && picture.isSelect) View.VISIBLE else View.GONE

        if (isManager) {
            holder.selectView.show()
            holder.selectView.setImageResource(
                if (picture.isSelect) R.drawable.img_pic_set_ic_ok else R.drawable.img_pic_set_ic_d
            )
            holder.selectView.setOnClickListener { toggleSelection(holder, picture) }
        } else {
            holder.selectView.hide()
            holder.selectView.setOnClickListener(null)
        }

        holder.itemView.setOnClickListener {
            if (isManager) {
                toggleSelection(holder, picture)
            } else {
                onItemClickListener?.onItemClick(
                    pictureList,
                    picture,
                    pictureList.indexOf(picture)
                )
            }
        }
    }

    private fun toggleSelection(holder: PhotoHolder, picture: Picture) {
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        picture.isSelect = !picture.isSelect
        notifyItemChanged(position)
        notifySelectCount()
    }

    private fun notifySelectCount() {
        onSelectItemListener?.onSelectNum(getSelectCount())
    }

    override fun getItemCount(): Int = displayItems.size

    fun getList(): List<Picture> = pictureList

    fun getSelectCount(): Int = pictureList.count { it.isSelect }

    class HeaderHolder(val dateView: TextView) : RecyclerView.ViewHolder(dateView)

    class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pictureView: ImageView = itemView.findViewById(R.id.iv_picture)
        val selectView: ImageView = itemView.findViewById(R.id.iv_select)
        val selectedOverlay: View? = itemView.findViewById(R.id.view_selected_overlay)
        val nameView: TextView? = itemView.findViewById(R.id.tv_name)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int)
    }

    fun setOnSelectItemListener(listener: OnSelectItemListener) {
        onSelectItemListener = listener
    }

    interface OnSelectItemListener {
        fun onSelectNum(num: Int)
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
    }
}


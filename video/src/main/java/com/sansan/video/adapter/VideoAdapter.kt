package com.sansan.video.adapter

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
import com.bumptech.glide.request.RequestOptions
import com.sansan.video.R
import com.sansan.video.model.Video
import com.sansan.video.utils.hide
import com.sansan.video.utils.show
import com.sansan.video.utils.toChineseDate

class VideoAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val isKy1045 = context.resources.getBoolean(R.bool.is_ky1045)
    private var videoList: List<Video> = emptyList()
    private var displayItems: List<Item> = emptyList()
    private var isManager = false
    private var onItemClickListener: OnItemClickListener? = null
    private var onSelectItemListener: OnSelectItemListener? = null

    private sealed class Item { data class Header(val date: String, val count: Int) : Item(); data class Entry(val video: Video) : Item() }

    fun setData(list: List<Video>) {
        videoList = if (isKy1045) list.sortedByDescending { it.dateTaken } else list
        displayItems = if (isKy1045) buildList {
            videoList.groupBy { it.dateTaken.toChineseDate() }.forEach { (date, videos) ->
                add(Item.Header(date, videos.size)); videos.forEach { add(Item.Entry(it)) }
            }
        } else videoList.map { Item.Entry(it) }
        notifyDataSetChanged()
    }

    fun setManager(manager: Boolean) { isManager = manager; notifyDataSetChanged(); notifyCount() }
    fun setAllSelect() { videoList.forEach { it.isSelect = true }; notifyDataSetChanged(); notifyCount() }
    fun setCancelManager() { videoList.forEach { it.isSelect = false }; isManager = false; notifyDataSetChanged(); notifyCount() }
    override fun getItemViewType(position: Int) = if (displayItems[position] is Item.Header) HEADER else VIDEO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == HEADER) HeaderHolder(inflater.inflate(context.resources.getIdentifier("layout_video_date_header", "layout", context.packageName), parent, false) as TextView)
        else VideoHolder(inflater.inflate(R.layout.layout_video_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is Item.Header -> (holder as HeaderHolder).view.text = "${item.date}（${item.count}）"
            is Item.Entry -> bindVideo(holder as VideoHolder, item.video)
        }
    }

    private fun bindVideo(holder: VideoHolder, video: Video) {
        Glide.with(context).asBitmap().load(video.path).transform(MultiTransformation(CenterCrop(), RoundedCorners(8)))
            .apply(RequestOptions().frame(0).override(256, 144)).into(holder.image)
        holder.duration.text = String.format("%02d:%02d", video.duration / 60000, video.duration / 1000 % 60)
        if (isKy1045) holder.name?.text = video.name.takeIf { it.isNotBlank() } ?: video.dateTaken.toChineseDate()
        holder.overlay?.visibility = if (isKy1045 && isManager && video.isSelect) View.VISIBLE else View.GONE
        if (isManager) {
            holder.select.show()
            holder.select.setImageResource(if (video.isSelect) R.drawable.img_pic_set_ic_ok else R.drawable.img_pic_set_ic_d)
        } else holder.select.hide()
        val toggle = View.OnClickListener { toggle(holder, video) }
        holder.select.setOnClickListener(if (isManager) toggle else null)
        holder.itemView.setOnClickListener { if (isManager) toggle(holder, video) else onItemClickListener?.onItemClick(videoList, video, videoList.indexOf(video)) }
    }

    private fun toggle(holder: VideoHolder, video: Video) { val position = holder.bindingAdapterPosition; if (position == RecyclerView.NO_POSITION) return; video.isSelect = !video.isSelect; notifyItemChanged(position); notifyCount() }
    private fun notifyCount() = onSelectItemListener?.onSelectNum(getSelectCount())
    override fun getItemCount() = displayItems.size
    fun getList() = videoList
    fun getSelectCount() = videoList.count { it.isSelect }
    class HeaderHolder(val view: TextView) : RecyclerView.ViewHolder(view)
    class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_picture)
        val select: ImageView = itemView.findViewById(R.id.iv_select)
        val duration: TextView = itemView.findViewById(R.id.tv_duration)
        val overlay: View? = itemView.findViewById(R.id.view_selected_overlay)
        val name: TextView? = itemView.findViewById(R.id.tv_name)
    }
    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }
    interface OnItemClickListener { fun onItemClick(videoList: List<Video>, bean: Video, position: Int) }
    fun setOnSelectItemListener(listener: OnSelectItemListener) { onSelectItemListener = listener }
    interface OnSelectItemListener { fun onSelectNum(num: Int) }
    companion object { private const val HEADER = 0; private const val VIDEO = 1 }
}


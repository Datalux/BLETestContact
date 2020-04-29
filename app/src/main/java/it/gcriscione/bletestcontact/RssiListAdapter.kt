package it.gcriscione.bletestcontact

import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import java.util.*
import kotlin.math.pow


class RssiListAdapter(var context: Context) :
    RecyclerView.Adapter<RssiListAdapter.ViewHolder>() {
    class ViewHolder(v: View?, var tvName: TextView, var tvLevel: TextView) :
        RecyclerView.ViewHolder(v!!)

    class ScanValue {
        var uuid: String? = null
        var rssi = 0
    }

    var defColors: ColorStateList? = null
    var dataset: SortedList<ScanValue?>
    fun add(result: ScanResult) {
        val uuid: String? = Utils.getEddystoneUid(result.scanRecord!!.bytes)
        Log.d("RssiListAdapter", uuid.toString())
        if (uuid != null) {
            val value = ScanValue()
            value.rssi = result.rssi
            value.uuid = uuid
            dataset.add(value)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        val tvName = TextView(context)
        tvName.typeface = Typeface.MONOSPACE
        tvName.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        val tvLevel = TextView(context)
        tvLevel.typeface = Typeface.MONOSPACE
        tvLevel.textSize = 20f
        tvLevel.setPadding(10, 0, 0, 30)
        defColors = tvLevel.textColors
        layout.addView(tvName)
        layout.addView(tvLevel)
        return ViewHolder(layout, tvName, tvLevel)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val scan = dataset[position]!!

        // Convert RSSI to distance in meters
        val distance = 10.0.pow((R0 - scan.rssi) / (N * 10))
        holder.tvName.text = scan.uuid
        holder.tvName.setBackgroundColor(0x00)
        holder.tvLevel.text = String.format(
            Locale.US,
            "RSSI=%ddBm dist=%.1fm",
            scan.rssi,
            distance
        )
        if (scan.rssi > RSSI_THRESHOLD) {
            holder.tvLevel.setTextColor(-0x10000)
        } else {
            holder.tvLevel.setTextColor(defColors)
        }
    }

    override fun getItemCount(): Int {
        return dataset.size()
    }

    companion object {
        const val RSSI_THRESHOLD = -65
        const val R0 = -65.0
        const val N = 3.0
    }

    init {
        // We use a sorted list to avoid duplicates
        dataset = SortedList(
            ScanValue::class.java,
            object : SortedListAdapterCallback<ScanValue?>(this) {
                override fun compare(o1: ScanValue?, o2: ScanValue?): Int {
                    return o1?.uuid!!.compareTo(o2?.uuid!!)
                }

                override fun areContentsTheSame(oldItem: ScanValue?, newItem: ScanValue?): Boolean {
                    return oldItem?.rssi == newItem?.rssi
                }

                override fun areItemsTheSame(item1: ScanValue?, item2: ScanValue?): Boolean {
                    return item1?.uuid == item2?.uuid
                }
            })
    }
}
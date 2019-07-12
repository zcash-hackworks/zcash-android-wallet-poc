package cash.z.android.wallet.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cash.z.android.wallet.R
import cash.z.android.wallet.extention.toAppColor
import cash.z.android.wallet.extention.toRelativeTimeString
import cash.z.android.wallet.extention.truncate
import cash.z.wallet.sdk.dao.ClearedTransaction
import cash.z.wallet.sdk.ext.MINERS_FEE_ZATOSHI
import cash.z.wallet.sdk.ext.convertZatoshiToZecString
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue


class TransactionAdapter(@LayoutRes val itemResId: Int = R.layout.item_transaction) : ListAdapter<ClearedTransaction, TransactionViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(itemResId, parent, false)
        return TransactionViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) = holder.bind(getItem(position))
}

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClearedTransaction>() {
    override fun areItemsTheSame(oldItem: ClearedTransaction, newItem: ClearedTransaction) = oldItem.height == newItem.height
    override fun areContentsTheSame(oldItem: ClearedTransaction, newItem: ClearedTransaction) = oldItem == newItem
}

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val status = itemView.findViewById<View>(R.id.view_transaction_status)
    private val icon = itemView.findViewById<ImageView>(R.id.image_transaction_type)
    private val timestamp = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val amount = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val address = itemView.findViewById<TextView>(R.id.text_transaction_address)
    private val memo = itemView.findViewById<TextView>(R.id.text_transaction_memo)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    fun bind(tx: ClearedTransaction) {
        val isChip = tx.isPokerChip()
        val isSwag = tx.isSend && (tx.memo ?: "").toLowerCase().contains("swag")
        val useSend = tx.isSend && !isChip
        val isHistory = icon != null
        val sign = if (useSend) "- " else "+ "
        val amountColor = if (useSend) R.color.colorAccent else R.color.zcashPurple_accent
        val transactionColor = if (useSend) R.color.send_associated else R.color.receive_associated
        val transactionIcon = if (useSend || (isChip && tx.address != "Redeemed")) R.drawable.ic_sent_transaction else R.drawable.ic_received_transaction
        val zecAbsoluteValue = tx.value.absoluteValue + if(tx.isSend) MINERS_FEE_ZATOSHI else 0
        val toOrFrom = if (useSend) "to" else "from"
        val srcOrDestination = tx.address?.truncate() ?: "shielded address"

        fun createActionText(): Pair<String, Int> {
            return when {
                tx.isSend && isChip -> "Scan" to R.color.text_light_dimmed
                tx.isMined && !(tx.isSend && tx.noteId <= 0) -> "$sign${zecAbsoluteValue.convertZatoshiToZecString(2)}" to amountColor
                isSwag -> "Purchase" to R.color.text_light_dimmed
                else -> (if (tx.isSend) "Transfer" else "Pending") to R.color.text_light_dimmed
            }
        }

        val actionText = createActionText()
        timestamp.text = if (tx.timeInSeconds == 0L) "Pending"
                         else (if (isHistory) formatter.format(tx.timeInSeconds * 1000) else (tx.timeInSeconds * 1000L).toRelativeTimeString())
        amount.text = actionText.first
        amount.setTextColor(actionText.second.toAppColor())

        // maybes - and if this gets to be too much, then pass in a custom holder when constructing the adapter, instead
        status?.setBackgroundColor(transactionColor.toAppColor())
        address?.text = if (tx.isSend) {
            if (tx.isMined) {
                if (isSwag && tx.isMined && tx.status == null) "Purchase accepted."
                else "Transfer complete."
            } else tx.status
        } else "$toOrFrom $srcOrDestination"
        memo?.text = tx.memo
        icon?.setImageResource(transactionIcon)
    }
}

private fun ClearedTransaction.isPokerChip(): Boolean {
    return memo?.contains("Poker Chip") == true
}

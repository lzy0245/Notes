package com.example.notes.adapters

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.activities.CreateNoteActivity
import com.example.notes.entities.Note
import com.example.notes.listener.NotesListener
import com.makeramen.roundedimageview.RoundedImageView
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Handler
import java.util.logging.LogRecord

class NotesAdapter: RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private var notes: List<Note>
    private var notesListener: NotesListener
    private var timer: Timer? = null
    private var notesSource: List<Note>

    constructor(notes :List<Note>, notesListener: NotesListener) {
        this.notes = notes
        this.notesListener = notesListener
        this.notesSource = notes
    }
    //onCreateViewHolder()方法是用于创建NoteViewHolder实例的，我们在这个方法中将
    //item_container_note布局加载进来，然后创建一个NoteViewHolder实例，并把加载出来的布局传入构造
    //函数当中，最后将NoteViewHolder的实例返回。
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_container_note,
                parent,
                false
            )
        )
    }

    //onBindViewHolder()方法用于对
    //RecyclerView子项的数据进行赋值，会在每个子项被滚动到屏幕内的时候执行
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.setNote(notes[position])
        holder.layoutNote?.setOnClickListener {
            notesListener.onNoteClicked(notes[position],position)
        }
    }

    //返回长度
    override fun getItemCount(): Int {
        return notes.size
    }

    //返回位置
    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textTitle: TextView? = null
        var textSubtitle: TextView? = null
        var textDateTime: TextView? = null
        var layoutNote: LinearLayout? = null
        var itemImageNote: RoundedImageView? = null

        init {
            textTitle = itemView.findViewById(R.id.textTitle)
            textSubtitle = itemView.findViewById(R.id.textSubtitle)
            textDateTime = itemView.findViewById(R.id.textDateTime)
            layoutNote = itemView.findViewById(R.id.layoutNote)
            itemImageNote = itemView.findViewById(R.id.itemImageNote)
        }

        fun setNote(note: Note) {
            textTitle?.text = note.title
            textDateTime?.text = note.dateTime
            if (note.subtitle?.isEmpty() == true) {
                textSubtitle?.visibility = View.GONE
            } else {
                textSubtitle?.text = note.subtitle
            }

            //笔记item颜色
            var gradientDrawable: GradientDrawable = layoutNote?.background as GradientDrawable
            if (note.color != null) {
                gradientDrawable.setColor(Color.parseColor(note.color))
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"))
            }

            //笔记item图片
            if (note.imagePath != null) {
                itemImageNote?.setImageBitmap(BitmapFactory.decodeFile(note.imagePath))
                itemImageNote?.visibility = View.VISIBLE
            } else {
                itemImageNote?.visibility = View.GONE
            }
        }
    }

    fun searchNotes(searchKeyword: String) {
        timer = Timer()

        timer!!.schedule(object : TimerTask() {
            @SuppressLint("NotifyDataSetChanged")
            override fun run() {
                if (searchKeyword.trim().isEmpty()) {
                    notes = notesSource
                } else {
                    var temp: ArrayList<Note> = ArrayList()
                    for (note: Note in notesSource) {
                        if (note.title?.contains(searchKeyword) == true || note.subtitle?.contains(
                                searchKeyword
                            ) == true || note.noteText?.contains(searchKeyword) == true
                        ) {
                            temp.add(note)
                        }
                    }
                    notes = temp
                }
                android.os.Handler(Looper.getMainLooper()).post(object : Runnable {
                    override fun run() {
                        notifyDataSetChanged()
                    }

                })
            }
        }, 1000)
    }

    fun cancelTimer() {
        timer?.cancel()
    }
}
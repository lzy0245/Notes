package com.example.notes.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.notes.DataClass.EverydayData
import com.example.notes.DataInterface.EverydayService
import com.example.notes.R
import com.example.notes.adapters.NotesAdapter
import com.example.notes.database.NotesDatabase
import com.example.notes.entities.Note
import com.example.notes.listener.NotesListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.ArrayList
import kotlin.collections.List

class MainActivity : AppCompatActivity(), NotesListener{

    val REQUEST_CODE_ADD_NOTE: Int = 1
    val REQUEST_CODE_UPDATE_NOTE = 2
    val REQUEST_CODE_SHOW_NOTES = 3

    private lateinit var notesRecyclerView: RecyclerView
    private var noteList = ArrayList<Note>()
    private lateinit var notesAdapter: NotesAdapter

    private var noteClickedPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //点击悬浮按钮，进入创建笔记页面
        val imageAddNoteMain: FloatingActionButton = findViewById(R.id.imageAddNoteMain)
        imageAddNoteMain.setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, CreateNoteActivity::class.java),
                REQUEST_CODE_ADD_NOTE
            )
        }

        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        notesRecyclerView.setOnClickListener {

        }

        //瀑布流，2列
        val layoutManager = StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        notesRecyclerView.layoutManager = layoutManager
        notesAdapter = NotesAdapter(noteList,this)
        notesRecyclerView.adapter = notesAdapter

        //得到并展示所有note实体
        getNotes(REQUEST_CODE_SHOW_NOTES,false)

        //搜索
        val inputSearch: EditText = findViewById(R.id.inputSearch)
        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (noteList.size != 0) {
                    notesAdapter.searchNotes(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                notesAdapter.cancelTimer()
            }
        })

        initEveryday()
        getEverydayData()
    }

    //每日一句框初始化
    @SuppressLint("SetTextI18n")
    private fun initEveryday() {

        //上拉效果
        val layoutEveryday: LinearLayout = findViewById(R.id.layoutEveryday)
        val bottomSheetBehavior: BottomSheetBehavior<LinearLayout> =
            BottomSheetBehavior.from(layoutEveryday)
        layoutEveryday.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNoteClicked(note: Note, position: Int) {
        noteClickedPosition = position
        val intent: Intent = Intent(applicationContext,CreateNoteActivity::class.java)
        intent.putExtra("isViewOrUpdate",true)
        intent.putExtra("note",note)
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE)
    }

    //将NoteDatabase中的note取出
    private fun getNotes(requestCode: Int, isNoteDeleted: Boolean) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask : AsyncTask<Void?, Void?, List<Note>>() {

            //执行耗时操作，得到数据库中的所有note实体
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg p0: Void?): List<Note>? {
                return NotesDatabase.getDatabase(applicationContext)!!.noteDao()!!.getAllNotes()
            }

            //UI更新操作
            @SuppressLint("NotifyDataSetChanged")
            @Deprecated("Deprecated in Java")
            override fun onPostExecute(notes: List<Note>) {
                super.onPostExecute(notes)
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes)
                    notesAdapter.notifyDataSetChanged()
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes[0])
                    notesAdapter.notifyItemInserted(0)
                    notesRecyclerView.smoothScrollToPosition(0)
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.removeAt(noteClickedPosition)
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition)
                    } else {
                        noteList.add(noteClickedPosition, notes[noteClickedPosition])
                        notesAdapter.notifyItemChanged(noteClickedPosition)
                    }
                }
            }
        }
        GetNotesTask().execute()
    }

    //点击imageSave按钮，及时更新List<Note>,update
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE,false)
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false))
            }
        }
    }

    //Everyday Data
    private fun getEverydayData() {
        var map: Map<String, String>? = null
        val en: TextView? = findViewById(R.id.en)
        val zh: TextView? = findViewById(R.id.zh)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.vvhan.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val everydayService = retrofit.create(EverydayService::class.java)
        everydayService.getEverydayData("sj").enqueue(object : Callback<EverydayData> {
            override fun onResponse(call: Call<EverydayData>,
                                    response: Response<EverydayData>
            ) {
                val data: EverydayData? = response.body()
                if (data != null) {
                    en?.text = data.data.toString().substring(data.data.toString().indexOf("en")+3,data.data.toString().indexOf("pic")-2)
                    zh?.text = data.data.toString().substring(data.data.toString().indexOf("zh")+3,data.data.toString().indexOf("en")-2)
                }
            }
            override fun onFailure(call: Call<EverydayData>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}
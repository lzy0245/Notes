package com.example.notes.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.notes.R
import com.example.notes.database.NotesDatabase
import com.example.notes.entities.Note
import com.example.notes.view.BarChartView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import java.io.IOException

class CreateNoteActivity : AppCompatActivity() {

    private var inputNoteTitle: EditText? = null
    private var inputNoteSubtitle: EditText? = null
    private var inputNoteText: EditText? = null
    private var textDateTime: TextView? = null
    private var textSize: TextView? = null
    private var inputNote:EditText? = null
    private var viewSubtitleIndicator: View? = null
    private var imageNote: ImageView? = null
    private var voiceNote: LinearLayout? = null

    private var selectedNoteColor: String? = null
    private var selectedImagePath: String? = null

    private var REQUEST_CODE_STORAGE_PERMISSION: Int = 1
    private var REQUEST_CODE_SELECT_IMAGE: Int = 2
    private var REQUEST_CODE_RECORD_PERMISSION: Int = 1
    private var RECORD_STOP: Int = 1
    private var PLAY_STOP: Int = 1

    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var audioFileName: String? = null

    private var alreadyAvailableNote: Note? = null

    private var dialogDeleteNote: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        //????????????
        val imageBack: ImageView = findViewById(R.id.imageBack)
        imageBack.setOnClickListener { onBackPressed() }

        inputNoteTitle = findViewById(R.id.inputNoteTitle)
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle)
        inputNoteText = findViewById(R.id.inputNote)
        textDateTime = findViewById(R.id.textDateTime)
        textSize = findViewById(R.id.textSize)
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator)
        imageNote = findViewById(R.id.imageNote)
        voiceNote = findViewById(R.id.voiceNote)

        //????????????
        textDateTime?.text = SimpleDateFormat("yyyy???MM???dd??? EEEE a HH:mm", Locale.CHINA).format(Date())

        //??????????????????
        inputNote = findViewById(R.id.inputNote)
        inputNote?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                var textsize: String? = s?.length.toString()
                textSize!!.text = "  |  "+textsize+"???"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        //??????????????????
        val imageSave: ImageView = findViewById(R.id.imageSave)
        imageSave.setOnClickListener { saveNote() }

        selectedNoteColor = "#333333"
        selectedImagePath = ""

        if (intent.getBooleanExtra("isViewOrUpdate",false)) {
            alreadyAvailableNote = intent.getSerializableExtra("note") as Note
            setViewOrUpdateNote()
        }

        //????????????
        findViewById<ImageView>(R.id.imageRemoveVoice).setOnClickListener {
            voiceNote?.visibility = View.GONE
            audioFileName = ""
        }

        //????????????
        findViewById<ImageView>(R.id.imageRemoveImage).setOnClickListener {
            imageNote?.setImageBitmap(null)
            imageNote?.visibility = View.GONE
            findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.GONE
            selectedImagePath = ""
        }
        //???????????????
        initMiscellaneous()

        //??????viewSubtitleIndicator??????
        setSubtitleIndicatorColor()
    }

    //???????????????
    private fun setViewOrUpdateNote() {
        inputNoteTitle?.setText(alreadyAvailableNote?.title)
        inputNoteSubtitle?.setText(alreadyAvailableNote?.subtitle)
        inputNoteText?.setText(alreadyAvailableNote?.noteText)
        textDateTime?.text = alreadyAvailableNote?.dateTime

        //???????????????
        if (alreadyAvailableNote?.imagePath != null && alreadyAvailableNote?.imagePath!!.trim().isNotEmpty()) {
            imageNote?.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote?.imagePath))
            imageNote?.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.VISIBLE
            selectedImagePath = alreadyAvailableNote?.imagePath
        }

        //???????????????
        if (alreadyAvailableNote?.voicePath != null && alreadyAvailableNote?.voicePath!!.trim().isNotEmpty()) {
            voiceNote?.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.imageRemoveVoice).visibility = View.VISIBLE
            audioFileName = alreadyAvailableNote?.voicePath
        }
    }

    //??????????????????
    private fun saveNote() {

        //?????????????????????????????????Toast?????????????????????
        if (inputNoteTitle?.text.toString().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show()
            return
        } else if (inputNoteSubtitle?.text.toString().isEmpty() && inputNoteText?.text.toString()
                .isEmpty()
        ) {
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        //????????????note????????????????????????????????????note
        val note = Note()
        note.title = inputNoteTitle?.text.toString()
        note.subtitle = inputNoteSubtitle?.text.toString()
        note.noteText = inputNoteText?.text.toString()
        note.dateTime = textDateTime?.text.toString()
        note.imagePath = selectedImagePath
        note.color = selectedNoteColor
        note.voicePath = audioFileName

        if (alreadyAvailableNote != null) {
            note.id = alreadyAvailableNote?.id!!
        }

        //???note????????????NoteDatabase???
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask : AsyncTask<Void?, Void?, Void?>() {

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg p0: Void?): Void? {
                NotesDatabase.getDatabase(applicationContext)!!.noteDao()!!.insertNote(note)
                return null
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(aVoid: Void?) {
                super.onPostExecute(aVoid)
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        SaveNoteTask().execute()
    }

    //????????????????????????
    @SuppressLint("SetTextI18n")
    private fun initMiscellaneous() {

        //????????????
        val layoutMiscellaneous: LinearLayout = findViewById(R.id.layoutMiscellaneous)
        val bottomSheetBehavior: BottomSheetBehavior<LinearLayout> =
            BottomSheetBehavior.from(layoutMiscellaneous)
        layoutMiscellaneous.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        //???????????????
        val imageColor1: ImageView = layoutMiscellaneous.findViewById(R.id.imageColor1)
        val imageColor2: ImageView = layoutMiscellaneous.findViewById(R.id.imageColor2)
        val imageColor3: ImageView = layoutMiscellaneous.findViewById(R.id.imageColor3)
        val imageColor4: ImageView = layoutMiscellaneous.findViewById(R.id.imageColor4)
        val imageColor5: ImageView = layoutMiscellaneous.findViewById(R.id.imageColor5)

        val viewColor1: View = layoutMiscellaneous.findViewById(R.id.viewColor1)
        val viewColor2: View = layoutMiscellaneous.findViewById(R.id.viewColor2)
        val viewColor3: View = layoutMiscellaneous.findViewById(R.id.viewColor3)
        val viewColor4: View = layoutMiscellaneous.findViewById(R.id.viewColor4)
        val viewColor5: View = layoutMiscellaneous.findViewById(R.id.viewColor5)

        viewColor1.setOnClickListener {
            selectedNoteColor = "#333333"
            imageColor1.setImageResource(R.drawable.ic_done)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            setSubtitleIndicatorColor()
        }
        viewColor2.setOnClickListener {
            selectedNoteColor = "#FDBE3B"
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(R.drawable.ic_done)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            setSubtitleIndicatorColor()
        }
        viewColor3.setOnClickListener {
            selectedNoteColor = "#FF4B42"
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(R.drawable.ic_done)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            setSubtitleIndicatorColor()
        }
        viewColor4.setOnClickListener {
            selectedNoteColor = "#3A52FC"
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(R.drawable.ic_done)
            imageColor5.setImageResource(0)
            setSubtitleIndicatorColor()
        }
        viewColor5.setOnClickListener {
            selectedNoteColor = "#000000"
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(R.drawable.ic_done)
            setSubtitleIndicatorColor()
        }

        if (alreadyAvailableNote != null && alreadyAvailableNote?.color != null && alreadyAvailableNote?.color!!.isNotEmpty()) {
            when (alreadyAvailableNote?.color) {
                "#FDBE3B" -> {
                    layoutMiscellaneous.findViewById<View>(R.id.viewColor2).performClick()
                }
                "#FF4B42" -> {
                    layoutMiscellaneous.findViewById<View>(R.id.viewColor3).performClick()
                }
                "#3A52FC" -> {
                    layoutMiscellaneous.findViewById<View>(R.id.viewColor4).performClick()
                }
                "#000000" -> {
                    layoutMiscellaneous.findViewById<View>(R.id.viewColor5).performClick()
                }
            }
        }

        //???????????????
        val layoutAddImage: LinearLayout = layoutMiscellaneous.findViewById(R.id.layoutAddImage)
        layoutAddImage.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            //????????????????????????????????????????????????
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //?????????????????????????????????????????? ActivityCompat.requestPermissions()???????????????????????????
                //requestPermissions()????????????3????????????????????????????????????Activity??????????????????????????? ?????????String????????????????????????????????????????????????????????????????????????????????????????????????????????? ????????????????????????????????????1
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                selectImage()
            }
        }

        //????????????
        val voiceNote: LinearLayout = findViewById(R.id.voiceNote)
        val voiceBar2: BarChartView = findViewById(R.id.voiceBar2)
        val textVoice: TextView = layoutMiscellaneous.findViewById(R.id.textVoice)
        val layoutVoiceBar: LinearLayout = layoutMiscellaneous.findViewById(R.id.layoutVoiceBar)
        val voiceBar: BarChartView = layoutMiscellaneous.findViewById(R.id.voiceBar)
        val layoutAddVoice: LinearLayout = layoutMiscellaneous.findViewById(R.id.layoutAddVoice)
        layoutAddVoice.setOnClickListener {
            //????????????????????????????????????
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //?????????????????????????????????????????? ActivityCompat.requestPermissions()???????????????????????????
                //requestPermissions()????????????3????????????????????????????????????Activity??????????????????????????? ?????????String????????????????????????????????????????????????????????????????????????????????????????????????????????? ????????????????????????????????????1
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(RECORD_AUDIO), REQUEST_CODE_RECORD_PERMISSION
                )
            } else { //?????????
                when (RECORD_STOP) { //???????????????????????????
                    1 -> {
                        textVoice.text = "Stop"
                        layoutVoiceBar.visibility = View.VISIBLE
                        voiceBar.start()
                        recordVoice()
                        RECORD_STOP = 0
                    }
                    0 -> {
                        textVoice.text = "Add Voice"
                        layoutVoiceBar.visibility = View.GONE
                        voiceNote.visibility = View.VISIBLE
                        voiceBar.stop()
                        recordVoice()
                        RECORD_STOP = 1
                    }
                }
            }
        }

        //????????????????????????
        val playVoiceView: ImageView = findViewById(R.id.playVoiceView)
        playVoiceView.setOnClickListener {
            when (PLAY_STOP) {
                1 -> {
                    voiceBar2.start()
                    playVoiceView.setImageResource(R.drawable.ic_stop)
                    playstopVoice()
                    PLAY_STOP = 0
                }
                0 -> {
                    voiceBar2.stop()
                    playVoiceView.setImageResource(R.drawable.ic_play)
                    playstopVoice()
                    PLAY_STOP = 1
                }
            }
            player?.setOnCompletionListener {
                voiceBar2.stop()
                playVoiceView.setImageResource(R.drawable.ic_play)
                playstopVoice()
                PLAY_STOP = 1
            }
        }

        //??????
        if (alreadyAvailableNote != null) {
            layoutMiscellaneous.findViewById<LinearLayout>(R.id.layoutDeleteNote).visibility = View.VISIBLE
            layoutMiscellaneous.findViewById<LinearLayout>(R.id.layoutDeleteNote).setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                showDeleteNoteDialog()
            }
        }

    }

    //?????????
    private fun showDeleteNoteDialog() {

        if (dialogDeleteNote == null) {
            //???????????????
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val view: View = LayoutInflater.from(this).inflate(
                R.layout.layout_delete_note,
                findViewById(R.id.layoutDeleteContainer)
            )
            builder.setView(view)
            dialogDeleteNote = builder.create()
            if (dialogDeleteNote!!.window != null) {
                dialogDeleteNote!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            //????????????
            view.findViewById<TextView>(R.id.textDeleteNote).setOnClickListener {
                class DeleteNoteTask : AsyncTask<Void, Void, Void>() {
                    @Deprecated("Deprecated in Java")
                    override fun doInBackground(vararg p0: Void?): Void? {
                        NotesDatabase.getDatabase(applicationContext)!!.noteDao()!!
                            .deleteNote(alreadyAvailableNote)
                        return null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onPostExecute(result: Void?) {
                        super.onPostExecute(result)
                        val intent: Intent = Intent()
                        intent.putExtra("isNoteDeleted", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
                DeleteNoteTask().execute()
            }
            //????????????
            view.findViewById<TextView>(R.id.textCancel).setOnClickListener {
                dialogDeleteNote!!.dismiss()
            }
        }
        dialogDeleteNote!!.show()
    }

    //??????viewSubtitleIndicator??????
    private fun setSubtitleIndicatorColor() {
        val gradientDrawable: GradientDrawable =
            viewSubtitleIndicator?.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor))
    }

    //?????????????????????
    @SuppressLint("QueryPermissionsNeeded")
    private fun selectImage() {

        //Intent.ACTION_PICK:
        // ????????????????????????????????????????????????????????????????????????????????????????????? intent.getData() ??????????????????
        // ??????????????? "content://" ????????? Uri ?????????????????? context.getContentResolver() ???????????????????????????????????????
        // ?????? MediaStore.Images ??????uri "content://com.android.providers.media.documents/document/images_bucket%3A540528482".toUri()
        val intent: Intent =
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        if (intent.resolveActivity(packageManager) != null) {
            //?????????????????????????????????Activity
            //??????startActivityForResult?????????requestCode???REQUEST_CODE_SELECT_IMAGE=2????????????????????????????????????
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
        }
    }

    //?????????requestPermissions()?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????onRequestPermissionsResult()?????????????????????????????????????????????grantResults???????????????
    //?????????????????????????????????????????????????????????????????????????????????????????????selectImage()?????????????????????????????????????????????????????????????????????????????????????????????
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectImage()
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_RECORD_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Allowed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //onActivityResult???????????????????????????????????????????????????????????????????????????????????????
    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            //????????????
            REQUEST_CODE_SELECT_IMAGE -> {
                //??????Uri????????????????????????????????????
                if (resultCode == RESULT_OK && data != null) {
                    val selectedImageUri: Uri? = data.data
                    if (selectedImageUri != null) {
                        try {
                            val inputStream: InputStream? =
                                contentResolver.openInputStream(selectedImageUri)
                            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                            imageNote?.setImageBitmap(bitmap)
                            imageNote?.visibility = View.VISIBLE
                            findViewById<ImageView>(R.id.imageRemoveImage).visibility = View.VISIBLE

                            selectedImagePath = getPathFromUri(selectedImageUri)
//                            Toast.makeText(this,selectedImageUri.path,Toast.LENGTH_SHORT).show()
                        } catch (exception: Exception) {
                            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }

    //??????Uri???????????????
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPathFromUri(contentUri: Uri): String? {
        var imageFilePath: String? = null
        val cursor: Cursor? = contentResolver.query(contentUri, null, null, null)
        if (cursor == null) {
            imageFilePath = contentUri.path
        } else {
            cursor.moveToFirst()
            val index: Int = cursor.getColumnIndex("_data")
            imageFilePath = cursor.getString(index)
            cursor.close()
        }
        return imageFilePath
    }

    //????????????
    private fun recordVoice() {
        when (RECORD_STOP) {
            1 -> {
                startRecording()
            }
            0 -> {
                stopRecording()
            }
        }
    }

    //??????????????????????????????
    private fun playstopVoice() {
        when (PLAY_STOP) {
            1 -> { //??????
                startPlaying()
            }
            0 -> { //??????
                stopPlaying()
            }
        }
    }

    //??????
    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(audioFileName)
                prepare()
                start()
            } catch (e: IOException) {
                Toast.makeText(CreateNoteActivity(),"prepare() failed!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    //????????????
    private fun stopPlaying() {
        player?.release()
        player = null
    }

    //??????
    private fun startRecording() {
        //??????????????????
        audioFileName = "${externalCacheDir?.absolutePath}/${SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())}.3gp"

        //??????
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Toast.makeText(CreateNoteActivity(),"prepare() failed!",Toast.LENGTH_SHORT).show()
            }

            start()
        }
    }

    //????????????
    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        /*externalCacheDir?.absolutePath+ (externalCacheDir?.absolutePath?.let {
            audioFileName?.replace(
                it,"")
        }
            ?: String)*/
        recorder = null
        Toast.makeText(this, "??????????????????$audioFileName", Toast.LENGTH_SHORT).show()
    }

}
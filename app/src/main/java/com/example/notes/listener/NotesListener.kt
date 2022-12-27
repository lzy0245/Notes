package com.example.notes.listener

import android.content.Intent
import com.example.notes.entities.Note

interface NotesListener {
    fun onNoteClicked(note: Note, position: Int)
}
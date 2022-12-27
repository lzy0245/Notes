package com.example.notes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notes.Dao.NoteDao
import com.example.notes.entities.Note

@Database(version = 1, entities = [Note::class])
abstract class NotesDatabase: RoomDatabase() {

    abstract fun noteDao(): NoteDao?

    companion object {
        private var notesDatabase: NotesDatabase? = null
        @Synchronized
        fun getDatabase(context: Context?): NotesDatabase? {
            if (notesDatabase == null) {
                notesDatabase = Room.databaseBuilder(
                    context!!,
                    NotesDatabase::class.java,
                    "notes_db"
                ).build()
            }
            return notesDatabase
        }
    }
}
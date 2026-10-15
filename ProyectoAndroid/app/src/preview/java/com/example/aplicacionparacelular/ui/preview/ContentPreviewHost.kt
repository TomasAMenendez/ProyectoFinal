package com.example.aplicacionparacelular.ui.preview

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentStoriesBinding
import com.example.aplicacionparacelular.databinding.FragmentStoryFormBinding
import com.example.aplicacionparacelular.ui.routines.RoutineDeleteDialog
import com.example.aplicacionparacelular.ui.stories.CatalogLoad
import com.example.aplicacionparacelular.ui.stories.ContentFiles
import com.example.aplicacionparacelular.ui.stories.ContentPickers
import com.example.aplicacionparacelular.ui.stories.ContentRules
import com.example.aplicacionparacelular.ui.stories.ContentSection
import com.example.aplicacionparacelular.ui.stories.FileSummary
import com.example.aplicacionparacelular.ui.stories.SongItem
import com.example.aplicacionparacelular.ui.stories.StoryItem
import com.google.android.material.snackbar.Snackbar

enum class ContentPreviewScenario(val labelRes: Int) {
    PLAYING(R.string.preview_content_playing),
    UPLOADING(R.string.preview_content_uploading),
    EMPTY(R.string.preview_content_empty),
    ERROR(R.string.preview_content_error),
    OFFLINE(R.string.preview_content_offline),
}

class ContentPreviewHost(
    private val activity: ComponentActivity,
    private val onAlerts: () -> Unit,
    private val onConfig: () -> Unit,
    private val onFormVisible: (Boolean) -> Unit,
) {
    private val list = FragmentStoriesBinding.inflate(activity.layoutInflater)
    private val form = FragmentStoryFormBinding.inflate(activity.layoutInflater)
    private var section = ContentSection.MUSIC
    private var scenario = ContentPreviewScenario.PLAYING
    private var activeSong = "musica_para_jugar.mp3"
    private var activeStory: String? = null
    private var pickedUpload: FileSummary? = null
    private var pdfName: String? = null
    private var pdfSize: Long = -1
    private var editing = false
    private var restoredForm = false

    private val audioPicker = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        ContentFiles.describe(activity, uri, "cancion.mp3") { summary ->
            if (summary == null) {
                snack(activity.getString(R.string.content_read_failed))
                return@describe
            }
            val problem = ContentRules.audioProblem(summary.name)
            if (problem != null) {
                snack(problem)
                return@describe
            }
            pickedUpload = summary
            render()
            simulated()
        }
    }

    private val pdfPicker = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        ContentFiles.describe(activity, uri, "cuento.pdf") { summary ->
            if (!isFormVisible) return@describe
            if (summary == null) {
                showFormError(activity.getString(R.string.content_read_failed))
                return@describe
            }
            val problem = ContentRules.pdfProblem(summary.name, summary.sizeBytes)
            if (problem != null) {
                showFormError(problem)
                return@describe
            }
            pdfName = summary.name
            pdfSize = summary.sizeBytes
            form.txtFormError.isVisible = false
            paintPdf()
        }
    }

    val root: FrameLayout = FrameLayout(activity).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        addView(list.root)
        addView(form.root)
    }
    val isFormVisible: Boolean
        get() = form.root.visibility == View.VISIBLE

    val scenarioLabel: String
        get() = activity.getString(scenario.labelRes)

    fun showList() {
        form.root.visibility = View.GONE
        list.root.visibility = View.VISIBLE
        onFormVisible(false)
        render()
    }

    fun chooseScenario(next: ContentPreviewScenario) {
        scenario = next
        if (next == ContentPreviewScenario.PLAYING) {
            activeSong = "musica_para_jugar.mp3"
            activeStory = null
        }
        render()
    }

    init {
        list.tabMusic.setOnClickListener {
            section = ContentSection.MUSIC
            render()
        }
        list.tabStories.setOnClickListener {
            section = ContentSection.STORIES
            render()
        }
        list.btnAlertsBell.setOnClickListener { onAlerts() }
        list.btnMyTeo.setOnClickListener { onConfig() }
        list.btnAdd.setOnClickListener {
            if (!list.btnAdd.isEnabled) return@setOnClickListener
            if (section == ContentSection.STORIES) openForm(null) else audioPicker.launch(ContentPickers.audio())
        }
        list.btnStop.setOnClickListener {
            simulated()
            activeSong = ""
            activeStory = null
            render()
        }
        form.btnBack.setOnClickListener { showList() }
        form.btnCancel.setOnClickListener { showList() }
        form.btnChangePdf.setOnClickListener { pdfPicker.launch(ContentPickers.pdf()) }
        form.btnSave.setOnClickListener { submitForm() }
        showList()
    }

    private fun openForm(story: StoryItem?) {
        val edit = story != null
        form.root.visibility = View.VISIBLE
        list.root.visibility = View.GONE
        onFormVisible(true)
        form.txtFormTitle.setText(if (edit) R.string.content_edit_story else R.string.content_add_story)
        form.txtFormIntro.setText(if (edit) R.string.content_edit_story_intro else R.string.content_add_story_intro)
        form.cardPdf.isVisible = !edit
        form.groupCreateNotes.isVisible = !edit
        form.txtTextLabel.isVisible = edit
        form.inputText.isVisible = edit
        form.txtTitleHelp.setText(if (edit) R.string.content_title_edit_help else R.string.content_title_create_help)
        form.btnSave.setText(if (edit) R.string.routines_save_changes else R.string.content_upload_story)
        form.txtFormError.isVisible = false
        editing = story != null
        if (story != null) {
            form.editTitle.setText(story.title)
            form.editText.setText(activity.getString(R.string.preview_story_text))
        } else {
            pdfName = null
            pdfSize = -1
            form.editTitle.setText("")
            paintPdf()
        }
    }

    fun persist(out: Bundle) {
        out.putBoolean(KEY_FORM, isFormVisible)
        out.putBoolean(KEY_EDIT, editing)
        out.putString(KEY_PDF_NAME, pdfName)
        out.putLong(KEY_PDF_SIZE, pdfSize)
        out.putString(KEY_TITLE, form.editTitle.text?.toString())
        out.putString(KEY_UPLOAD_NAME, pickedUpload?.name)
        out.putLong(KEY_UPLOAD_SIZE, pickedUpload?.sizeBytes ?: -1L)
        out.putString(KEY_SECTION, section.name)
    }

    fun restore(state: Bundle?) {
        if (state == null) return
        section = runCatching { ContentSection.valueOf(state.getString(KEY_SECTION).orEmpty()) }
            .getOrDefault(ContentSection.MUSIC)
        val uploadName = state.getString(KEY_UPLOAD_NAME)
        val uploadSize = state.getLong(KEY_UPLOAD_SIZE, -1L)
        pickedUpload = if (!uploadName.isNullOrBlank() && uploadSize >= 0) FileSummary(uploadName, uploadSize) else null
        pdfName = state.getString(KEY_PDF_NAME)?.takeIf { it.isNotBlank() }
        pdfSize = if (pdfName == null) -1 else state.getLong(KEY_PDF_SIZE, -1L)
        editing = state.getBoolean(KEY_EDIT)
        restoredForm = state.getBoolean(KEY_FORM)
        if (restoredForm && !editing) {
            form.editTitle.setText(state.getString(KEY_TITLE).orEmpty())
            paintPdf()
        }
    }

    fun showRestoredForm() {
        if (!restoredForm) return
        restoredForm = false
        if (editing) return
        form.root.visibility = View.VISIBLE
        list.root.visibility = View.GONE
        onFormVisible(true)
        paintPdf()
    }

    private fun render() {
        val music = section == ContentSection.MUSIC
        val connected = scenario != ContentPreviewScenario.OFFLINE
        val uploading = scenario == ContentPreviewScenario.UPLOADING && music
        val load = if (scenario == ContentPreviewScenario.ERROR) CatalogLoad.FAILED else CatalogLoad.READY
        val songs = if (scenario == ContentPreviewScenario.EMPTY || scenario == ContentPreviewScenario.ERROR) {
            emptyList()
        } else {
            sampleSongs()
        }
        val stories = if (scenario == ContentPreviewScenario.EMPTY || scenario == ContentPreviewScenario.ERROR) {
            emptyList()
        } else {
            sampleStories()
        }
        val primary = ContextCompat.getColor(activity, R.color.teo_primary)
        val secondary = ContextCompat.getColor(activity, R.color.teo_secondary)
        list.txtTabMusic.setTextColor(if (music) primary else secondary)
        list.txtTabStories.setTextColor(if (music) secondary else primary)
        list.markMusic.setBackgroundColor(if (music) primary else android.graphics.Color.TRANSPARENT)
        list.markStories.setBackgroundColor(if (music) android.graphics.Color.TRANSPARENT else primary)
        list.txtSubtitle.setText(if (music) R.string.content_music_subtitle else R.string.content_stories_subtitle)
        list.btnAdd.setText(if (music) R.string.content_add_music else R.string.content_add_story)
        val realUpload = pickedUpload?.takeIf { music }
        val sampleUpload = uploading && realUpload == null
        list.btnAdd.isEnabled = connected && !sampleUpload
        list.cardUpload.isVisible = realUpload != null || sampleUpload
        list.txtUploadName.text = realUpload?.name ?: if (sampleUpload) "melodia_suave.mp3" else ""
        list.txtUploadSize.isVisible = realUpload != null
        if (realUpload != null) {
            list.txtUploadSize.text =
                "${ContentRules.formatLabel(realUpload.name)} · ${ContentRules.sizeLabel(realUpload.sizeBytes)}"
        }
        list.txtUploadHint.setText(
            if (realUpload != null) R.string.preview_simulated else R.string.content_uploading_hint,
        )
        list.txtNowLabel.setText(if (music) R.string.content_now_music else R.string.content_now_story)
        list.imgNow.setImageResource(if (music) R.drawable.teo_ic_music else R.drawable.teo_ic_book)
        val active = if (music) activeSong.takeIf { it.isNotBlank() && scenario == ContentPreviewScenario.PLAYING } else activeStory
        list.txtNow.text = active ?: activity.getString(if (music) R.string.content_nothing_playing else R.string.content_nothing_reading)
        val canStop = connected && !active.isNullOrBlank()
        list.btnStop.isEnabled = canStop
        val tone = ContextCompat.getColor(activity, if (canStop) R.color.teo_primary else R.color.teo_outline)
        list.btnStop.setTextColor(tone)
        list.btnStop.strokeColor = android.content.res.ColorStateList.valueOf(tone)
        list.btnStop.iconTint = android.content.res.ColorStateList.valueOf(tone)
        val count = if (music) songs.size else stories.size
        val notice = ContentRules.catalogNotice(
            connected,
            load,
            count,
            activity.getString(if (music) R.string.content_music_empty else R.string.content_stories_empty),
            activity.getString(if (music) R.string.content_music_error else R.string.content_stories_error),
            activity.getString(R.string.content_offline),
        )
        list.txtCatalogNotice.isVisible = notice != null
        list.txtCatalogNotice.text = notice.orEmpty()
        list.cardMusic.isVisible = music && songs.isNotEmpty()
        list.storyList.isVisible = !music && stories.isNotEmpty()
        list.progressCatalog.isVisible = false
        fillSongs(songs, active, connected)
        fillStories(stories, connected)
    }

    private fun fillSongs(songs: List<SongItem>, active: String?, connected: Boolean) {
        val container = list.musicList
        container.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        songs.forEachIndexed { index, song ->
            if (index > 0) {
                container.addView(View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(ContextCompat.getColor(activity, R.color.teo_outline))
                })
            }
            val row = inflater.inflate(R.layout.item_music_row, container, false)
            row.findViewById<TextView>(R.id.txt_song_name).text = ContentRules.wrappedName(song.filename)
            row.findViewById<TextView>(R.id.txt_song_meta).text =
                "${ContentRules.formatLabel(song.filename)} · ${ContentRules.sizeLabel(song.sizeBytes)}"
            val playing = song.filename == active
            row.findViewById<View>(R.id.txt_playing).isVisible = playing
            val play = row.findViewById<View>(R.id.btn_play_song)
            play.isVisible = !playing
            play.isEnabled = connected
            play.alpha = if (connected) 1f else 0.4f
            val delete = row.findViewById<View>(R.id.btn_delete_song)
            delete.isEnabled = connected
            delete.alpha = if (connected) 1f else 0.4f
            play.setOnClickListener {
                simulated()
                activeSong = song.filename
                activeStory = null
                chooseScenario(ContentPreviewScenario.PLAYING)
            }
            delete.setOnClickListener {
                RoutineDeleteDialog.show(
                    activity,
                    song.filename,
                    title = activity.getString(R.string.content_delete_music),
                    question = ContentRules.deleteQuestion(song.filename),
                ) { simulated() }
            }
            container.addView(row)
        }
    }

    private fun fillStories(stories: List<StoryItem>, connected: Boolean) {
        val container = list.storyList
        container.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        stories.forEach { story ->
            val row = inflater.inflate(R.layout.item_story_card, container, false)
            row.findViewById<TextView>(R.id.txt_story_title).text = story.title
            row.findViewById<TextView>(R.id.txt_story_words).text = activity.getString(R.string.content_words, story.wordCount)
            val read = row.findViewById<View>(R.id.btn_read_story)
            read.isEnabled = connected
            read.alpha = if (connected) 1f else 0.4f
            read.setOnClickListener {
                simulated()
                activeStory = story.title
                activeSong = ""
                render()
            }
            row.findViewById<View>(R.id.btn_edit_story).setOnClickListener { openForm(story) }
            val delete = row.findViewById<View>(R.id.btn_delete_story)
            delete.isEnabled = connected
            delete.alpha = if (connected) 1f else 0.4f
            delete.setOnClickListener {
                RoutineDeleteDialog.show(
                    activity,
                    story.title,
                    title = activity.getString(R.string.content_delete_story),
                    question = ContentRules.deleteQuestion(story.title),
                ) { simulated() }
            }
            container.addView(row)
        }
    }

    private fun sampleSongs() = listOf(
        SongItem("musica_para_jugar.mp3", 3_355_443L, ""),
        SongItem("cancion_para_descansar_con_un_nombre_largo.mp3", 5_033_165L, ""),
        SongItem("sonidos_suaves.wav", 2_202_010L, ""),
    )

    private fun sampleStories() = listOf(
        StoryItem("bosque", "El bosque de las luces", 650),
        StoryItem("nube", "La nube viajera que cruza el valle entero", 480),
        StoryItem("luna", "Un abrazo para Luna", 520),
    )

    private fun submitForm() {
        if (editing) {
            simulated()
            showList()
            return
        }
        val name = pdfName
        if (name == null) {
            showFormError(activity.getString(R.string.content_pdf_empty))
            return
        }
        val problem = ContentRules.pdfProblem(name, pdfSize)
        if (problem != null) {
            showFormError(problem)
            return
        }
        val titleProblem = ContentRules.createTitle(form.editTitle.text?.toString().orEmpty()).second
        if (titleProblem != null) {
            showFormError(titleProblem)
            return
        }
        form.txtFormError.isVisible = false
        simulated()
    }

    private fun paintPdf() {
        ContentPickers.bindPdf(form.txtPdfName, form.txtPdfMeta, form.btnChangePdf, pdfName, pdfSize)
    }

    private fun showFormError(message: String) {
        form.txtFormError.text = message
        form.txtFormError.isVisible = true
    }

    private fun snack(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun simulated() {
        Snackbar.make(root, R.string.preview_simulated, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val KEY_FORM = "preview_content_form"
        private const val KEY_EDIT = "preview_content_edit"
        private const val KEY_PDF_NAME = "preview_pdf_name"
        private const val KEY_PDF_SIZE = "preview_pdf_size"
        private const val KEY_TITLE = "preview_pdf_title"
        private const val KEY_UPLOAD_NAME = "preview_upload_name"
        private const val KEY_UPLOAD_SIZE = "preview_upload_size"
        private const val KEY_SECTION = "preview_content_section"
    }
}

package com.example.aplicacionparacelular.ui.stories

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentStoriesBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import com.example.aplicacionparacelular.ui.routines.RoutineDeleteDialog
import com.google.android.material.snackbar.Snackbar

class StoriesFragment : Fragment() {
    private var _binding: FragmentStoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StoriesViewModel

    private val audioPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        ContentFiles.read(requireContext(), uri, "cancion.mp3") { picked ->
            if (_binding == null) return@read
            if (picked == null) {
                snack(getString(R.string.content_read_failed))
            } else {
                viewModel.uploadSong(picked.name, picked.bytes)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity())[StoriesViewModel::class.java]
        _binding = FragmentStoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyTop(binding.contentScroll)
        binding.tabMusic.setOnClickListener { show(ContentSection.MUSIC) }
        binding.tabStories.setOnClickListener { show(ContentSection.STORIES) }
        binding.btnAdd.setOnClickListener { onAdd() }
        binding.btnStop.setOnClickListener {
            if (viewModel.section == ContentSection.MUSIC) viewModel.stopMusic() else viewModel.stopStory()
        }
        binding.btnAlertsBell.setOnClickListener { findNavController().navigate(R.id.nav_alerts) }
        binding.btnMyTeo.setOnClickListener { findNavController().navigate(R.id.nav_config) }
        viewModel.songs.observe(viewLifecycleOwner) { render() }
        viewModel.stories.observe(viewLifecycleOwner) { render() }
        viewModel.musicLoad.observe(viewLifecycleOwner) { render() }
        viewModel.storyLoad.observe(viewLifecycleOwner) { render() }
        viewModel.playback.observe(viewLifecycleOwner) { render() }
        viewModel.connected.observe(viewLifecycleOwner) { render() }
        viewModel.uploadingName.observe(viewLifecycleOwner) { render() }
        viewModel.pending.observe(viewLifecycleOwner) { render() }
        viewModel.notice.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                snack(message)
                viewModel.consumeNotice()
            }
        }
        show(viewModel.section)
        if (viewModel.connected.value == true) {
            viewModel.loadSongs()
            viewModel.loadStories()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.connected.value == true) {
            viewModel.refreshStatus()
            if (viewModel.section == ContentSection.MUSIC) viewModel.loadSongs() else viewModel.loadStories()
        }
        render()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun show(section: ContentSection) {
        viewModel.section = section
        render()
    }

    private fun onAdd() {
        if (viewModel.connected.value != true || viewModel.uploadingName.value != null) return
        if (viewModel.section == ContentSection.MUSIC) {
            audioPicker.launch(ContentPickers.audio())
        } else {
            findNavController().navigate(R.id.nav_story_form, bundleOf(StoryFormFragment.ARG_MODE to StoryFormFragment.MODE_CREATE))
        }
    }

    private fun render() {
        if (_binding == null) return
        val music = viewModel.section == ContentSection.MUSIC
        val connected = viewModel.connected.value == true
        val pending = viewModel.pending.value == true
        val uploading = viewModel.uploadingName.value
        val playback = viewModel.playback.value ?: PlaybackSnapshot(null, null, null)
        paintTab(music)
        binding.txtSubtitle.setText(if (music) R.string.content_music_subtitle else R.string.content_stories_subtitle)
        binding.btnAdd.setText(if (music) R.string.content_add_music else R.string.content_add_story)
        binding.btnAdd.isEnabled = connected && uploading == null && !pending
        binding.cardUpload.isVisible = music && uploading != null
        binding.txtUploadName.text = uploading.orEmpty()
        binding.txtNowLabel.setText(if (music) R.string.content_now_music else R.string.content_now_story)
        binding.imgNow.setImageResource(if (music) R.drawable.teo_ic_music else R.drawable.teo_ic_book)
        val activeName = if (!connected) {
            null
        } else if (music) {
            playback.songName
        } else {
            playback.storyTitle
        }
        binding.txtNow.text = activeName ?: getString(if (music) R.string.content_nothing_playing else R.string.content_nothing_reading)
        val canStop = connected && !pending && !activeName.isNullOrBlank()
        binding.btnStop.isEnabled = canStop
        val tone = if (canStop) R.color.teo_primary else R.color.teo_outline
        val color = ContextCompat.getColor(requireContext(), tone)
        binding.btnStop.setTextColor(color)
        binding.btnStop.strokeColor = android.content.res.ColorStateList.valueOf(color)
        binding.btnStop.iconTint = android.content.res.ColorStateList.valueOf(color)
        val load = if (music) viewModel.musicLoad.value ?: CatalogLoad.LOADING else viewModel.storyLoad.value ?: CatalogLoad.LOADING
        val count = if (music) viewModel.songs.value.orEmpty().size else viewModel.stories.value.orEmpty().size
        binding.progressCatalog.isVisible = load == CatalogLoad.LOADING && count == 0 && connected
        val notice = ContentRules.catalogNotice(
            connected = connected,
            load = load,
            count = count,
            emptyText = getString(if (music) R.string.content_music_empty else R.string.content_stories_empty),
            errorText = getString(if (music) R.string.content_music_error else R.string.content_stories_error),
            offlineText = getString(R.string.content_offline),
        )
        binding.txtCatalogNotice.isVisible = notice != null
        binding.txtCatalogNotice.text = notice.orEmpty()
        binding.txtCatalogNotice.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (load == CatalogLoad.FAILED || !connected) R.color.teo_error else R.color.teo_secondary,
            ),
        )
        binding.cardMusic.isVisible = music && count > 0
        binding.storyList.isVisible = !music && count > 0
        if (music) fillMusic(activeName, connected && !pending) else fillStories(connected && !pending)
    }

    private fun paintTab(music: Boolean) {
        val primary = ContextCompat.getColor(requireContext(), R.color.teo_primary)
        val secondary = ContextCompat.getColor(requireContext(), R.color.teo_secondary)
        binding.txtTabMusic.setTextColor(if (music) primary else secondary)
        binding.txtTabStories.setTextColor(if (music) secondary else primary)
        binding.markMusic.setBackgroundColor(if (music) primary else android.graphics.Color.TRANSPARENT)
        binding.markStories.setBackgroundColor(if (music) android.graphics.Color.TRANSPARENT else primary)
    }

    private fun fillMusic(active: String?, actionsEnabled: Boolean) {
        val list = binding.musicList
        list.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        viewModel.songs.value.orEmpty().forEachIndexed { index, song ->
            if (index > 0) {
                list.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teo_outline))
                })
            }
            val row = inflater.inflate(R.layout.item_music_row, list, false)
            row.findViewById<TextView>(R.id.txt_song_name).text = ContentRules.wrappedName(song.filename)
            row.findViewById<TextView>(R.id.txt_song_meta).text =
                "${ContentRules.formatLabel(song.filename)} · ${ContentRules.sizeLabel(song.sizeBytes)}"
            val playing = song.filename == active
            row.findViewById<View>(R.id.txt_playing).isVisible = playing
            val play = row.findViewById<View>(R.id.btn_play_song)
            play.isVisible = !playing
            play.isEnabled = actionsEnabled
            play.alpha = if (actionsEnabled) 1f else 0.4f
            play.setOnClickListener { viewModel.playSong(song.filename) }
            val delete = row.findViewById<View>(R.id.btn_delete_song)
            delete.isEnabled = actionsEnabled
            delete.alpha = if (actionsEnabled) 1f else 0.4f
            delete.setOnClickListener {
                RoutineDeleteDialog.show(
                    requireContext(),
                    song.filename,
                    title = getString(R.string.content_delete_music),
                    question = ContentRules.deleteQuestion(song.filename),
                ) { viewModel.deleteSong(song.filename) }
            }
            list.addView(row)
        }
    }

    private fun fillStories(actionsEnabled: Boolean) {
        val list = binding.storyList
        list.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        viewModel.stories.value.orEmpty().forEach { story ->
            val row = inflater.inflate(R.layout.item_story_card, list, false)
            row.findViewById<TextView>(R.id.txt_story_title).text = story.title
            row.findViewById<TextView>(R.id.txt_story_words).text = getString(R.string.content_words, story.wordCount)
            val read = row.findViewById<View>(R.id.btn_read_story)
            read.isEnabled = actionsEnabled
            read.alpha = if (actionsEnabled) 1f else 0.4f
            read.setOnClickListener { viewModel.playStory(story.id) }
            row.findViewById<View>(R.id.btn_edit_story).setOnClickListener {
                findNavController().navigate(
                    R.id.nav_story_form,
                    bundleOf(
                        StoryFormFragment.ARG_MODE to StoryFormFragment.MODE_EDIT,
                        StoryFormFragment.ARG_ID to story.id,
                    ),
                )
            }
            val delete = row.findViewById<View>(R.id.btn_delete_story)
            delete.isEnabled = actionsEnabled
            delete.alpha = if (actionsEnabled) 1f else 0.4f
            delete.setOnClickListener {
                RoutineDeleteDialog.show(
                    requireContext(),
                    story.title,
                    title = getString(R.string.content_delete_story),
                    question = ContentRules.deleteQuestion(story.title),
                ) { viewModel.deleteStory(story.id) }
            }
            list.addView(row)
        }
    }

    private fun snack(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}

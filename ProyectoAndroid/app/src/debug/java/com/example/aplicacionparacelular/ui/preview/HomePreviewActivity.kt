package com.example.aplicacionparacelular.ui.preview

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentAlertsBinding
import com.example.aplicacionparacelular.databinding.FragmentDashboardBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.dashboard.HomeScreenRenderer
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterPresenter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomePreviewActivity : AppCompatActivity() {
    private lateinit var home: FragmentDashboardBinding
    private lateinit var alerts: FragmentAlertsBinding
    private lateinit var routines: RoutinePreviewHost
    private lateinit var content: ContentPreviewHost
    private lateinit var agenda: AgendaPreviewHost
    private lateinit var myTeo: MyTeoPreviewHost
    private lateinit var activityPreview: ActivityPreviewHost
    private lateinit var alertsPreview: AlertsPreviewHost
    private var selectedHomeState = HomePreviewState.AWAKE
    private var myTeoOrigin = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedHomeState = HomePreviewState.entries.firstOrNull {
            it.name == savedInstanceState?.getString(HOME_STATE_KEY)
        } ?: HomePreviewState.AWAKE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_home_preview)
        val root = findViewById<View>(R.id.preview_root)
        val background = ContextCompat.getColor(this, R.color.teo_background)
        window.statusBarColor = background
        window.navigationBarColor = ContextCompat.getColor(this, R.color.teo_surface)
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        SystemBarInsets.applyTop(root)

        val stage = findViewById<android.widget.FrameLayout>(R.id.preview_content)
        home = FragmentDashboardBinding.inflate(layoutInflater, stage, true)
        alerts = FragmentAlertsBinding.inflate(layoutInflater, stage, true)
        alerts.root.visibility = View.GONE
        routines = RoutinePreviewHost(
            activity = this,
            onAlerts = { showAlerts(findViewById(R.id.preview_bottom_nav)) },
            onConfig = { openMyTeo(connection = false) },
            onEditorVisible = { editing ->
                findViewById<View>(R.id.preview_bottom_nav).visibility =
                    if (editing) View.GONE else View.VISIBLE
            },
        )
        stage.addView(routines.root)
        routines.root.visibility = View.GONE
        content = ContentPreviewHost(
            activity = this,
            onAlerts = { showAlerts(findViewById(R.id.preview_bottom_nav)) },
            onConfig = { openMyTeo(connection = false) },
            onFormVisible = { visible ->
                findViewById<View>(R.id.preview_bottom_nav).visibility =
                    if (visible) View.GONE else View.VISIBLE
            },
        )
        stage.addView(content.root)
        content.root.visibility = View.GONE
        content.restore(savedInstanceState)
        agenda = AgendaPreviewHost(
            activity = this,
            onAlerts = { showAlerts(findViewById(R.id.preview_bottom_nav)) },
            onConfig = { openMyTeo(connection = false) },
            onFormVisible = { visible ->
                findViewById<View>(R.id.preview_bottom_nav).visibility =
                    if (visible) View.GONE else View.VISIBLE
            },
        )
        stage.addView(agenda.root)
        agenda.root.visibility = View.GONE
        agenda.restore(savedInstanceState)
        myTeo = MyTeoPreviewHost(this, stage) { costume ->
            TeoCharacterPresenter.applyWave(home.imgCharacter, costume)
        }
        activityPreview = ActivityPreviewHost(
            activity = this,
            parent = stage,
            onBack = { showHome(findViewById(R.id.preview_bottom_nav)) },
            onChanged = {
                if (activityPreview.root.visibility == View.VISIBLE) {
                    updatePreviewBanner(homeVisible = false, activityVisible = true)
                }
            },
        )
        activityPreview.root.visibility = View.GONE
        alertsPreview = AlertsPreviewHost(this, alerts) {
            if (alerts.root.visibility == View.VISIBLE) {
                updatePreviewBanner(homeVisible = false, alertsVisible = true)
            }
        }
        alerts.btnBack.setOnClickListener { showHome(findViewById(R.id.preview_bottom_nav)) }
        activityPreview.scenario = ActivityPreviewScenario.entries.firstOrNull {
            it.name == savedInstanceState?.getString(ACTIVITY_STATE_KEY)
        } ?: ActivityPreviewScenario.DATA
        activityPreview.days = savedInstanceState?.getInt(ACTIVITY_DAYS_KEY) ?: 7
        alertsPreview.scenario = AlertsPreviewScenario.entries.firstOrNull {
            it.name == savedInstanceState?.getString(ALERTS_STATE_KEY)
        } ?: AlertsPreviewScenario.EXAMPLES

        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        val tint = ContextCompat.getColorStateList(this, R.color.teo_nav_item)
        bottom.setBackgroundColor(ContextCompat.getColor(this, R.color.teo_surface))
        bottom.itemIconTintList = tint
        bottom.itemTextColor = tint
        SystemBarInsets.applyBottom(bottom)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    presentHome()
                    true
                }
                R.id.nav_routines -> {
                    presentRoutines()
                    true
                }
                R.id.nav_stories -> {
                    presentContent()
                    true
                }
                R.id.nav_medical -> {
                    presentAgenda()
                    true
                }
                else -> {
                    simulated(root)
                    false
                }
            }
        }
        bottom.selectedItemId = R.id.nav_dashboard
        if (savedInstanceState?.getBoolean("preview_content_form") == true) {
            bottom.selectedItemId = R.id.nav_stories
            content.showRestoredForm()
        }
        if (savedInstanceState?.getBoolean("preview_agenda_form") == true) {
            bottom.selectedItemId = R.id.nav_medical
            agenda.showRestoredForm()
        }
        myTeo.restore(savedInstanceState)

        bindHome()
        alertsPreview.render()
        activityPreview.render()
        if (myTeo.visible) {
            hideSections()
            bottom.visibility = View.GONE
            updatePreviewBanner(homeVisible = false, myTeoVisible = true)
        } else if (!content.isFormVisible && !agenda.isFormVisible) {
            showHome(bottom)
        }
        onBackPressedDispatcher.addCallback(this) {
            val bottomNav = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
            when {
                myTeo.back() -> {
                    if (!myTeo.visible) showOrigin(bottomNav)
                }
                routines.isEditing -> {
                    routines.showList()
                    bottomNav.visibility = View.VISIBLE
                }
                content.isFormVisible -> content.showList()
                agenda.isFormVisible -> {
                    agenda.showList()
                    bottomNav.visibility = View.VISIBLE
                }
                alerts.root.visibility == View.VISIBLE ||
                    activityPreview.root.visibility == View.VISIBLE ||
                    routines.root.visibility == View.VISIBLE ||
                    content.root.visibility == View.VISIBLE ||
                    agenda.root.visibility == View.VISIBLE -> showHome(bottomNav)
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun bindHome() {
        val root = findViewById<View>(R.id.preview_root)
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        home.btnPower.setOnClickListener { simulated(root) }
        home.btnMusicPlay.setOnClickListener { simulated(root) }
        home.btnMusicStop.setOnClickListener { simulated(root) }
        home.btnCelebrate.setOnClickListener { simulated(root) }
        home.btnMyTeo.setOnClickListener { openMyTeo(connection = false) }
        home.btnConnection.setOnClickListener { openMyTeo(connection = true) }
        home.btnActivity.setOnClickListener { showActivity() }
        home.btnContent.setOnClickListener { simulated(root) }
        home.btnAlertsBell.setOnClickListener { showAlerts(bottom) }
        home.btnAllAlerts.setOnClickListener { showAlerts(bottom) }
        home.txtWaiting.visibility = View.GONE
        renderHomeState()
        stackPlayback()
        home.imgCharacter.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left == oldRight - oldLeft && bottom - top == oldBottom - oldTop) return@addOnLayoutChangeListener
            TeoCharacterPresenter.applyWave(view as ImageView, myTeo.costume)
        }
    }

    private fun showActivity() {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        hideSections()
        myTeo.close()
        activityPreview.render()
        activityPreview.root.visibility = View.VISIBLE
        bottom.visibility = View.GONE
        updatePreviewBanner(homeVisible = false, activityVisible = true)
    }

    private fun showHome(bottom: BottomNavigationView) {
        if (bottom.selectedItemId != R.id.nav_dashboard) {
            bottom.selectedItemId = R.id.nav_dashboard
        } else {
            presentHome()
        }
    }

    private fun presentHome() {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        alerts.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        routines.root.visibility = View.GONE
        content.root.visibility = View.GONE
        agenda.root.visibility = View.GONE
        myTeo.close()
        home.root.visibility = View.VISIBLE
        bottom.visibility = View.VISIBLE
        updatePreviewBanner(homeVisible = true)
        stackPlayback()
        TeoCharacterPresenter.applyWave(home.imgCharacter, myTeo.costume)
    }

    private fun showAlerts(bottom: BottomNavigationView) {
        home.root.visibility = View.GONE
        routines.root.visibility = View.GONE
        content.root.visibility = View.GONE
        agenda.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        myTeo.close()
        alertsPreview.render()
        alerts.root.visibility = View.VISIBLE
        bottom.visibility = View.GONE
        updatePreviewBanner(homeVisible = false, alertsVisible = true)
    }

    private fun showRoutines(bottom: BottomNavigationView) {
        if (bottom.selectedItemId != R.id.nav_routines) {
            bottom.selectedItemId = R.id.nav_routines
        } else {
            presentRoutines()
        }
    }

    private fun presentRoutines() {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        home.root.visibility = View.GONE
        alerts.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        content.root.visibility = View.GONE
        agenda.root.visibility = View.GONE
        myTeo.close()
        routines.showList()
        routines.root.visibility = View.VISIBLE
        bottom.visibility = View.VISIBLE
        updatePreviewBanner(homeVisible = false)
    }

    private fun presentContent() {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        home.root.visibility = View.GONE
        alerts.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        routines.root.visibility = View.GONE
        agenda.root.visibility = View.GONE
        myTeo.close()
        content.showList()
        content.root.visibility = View.VISIBLE
        bottom.visibility = View.VISIBLE
        updatePreviewBanner(homeVisible = false, contentVisible = true)
    }

    private fun presentAgenda() {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        home.root.visibility = View.GONE
        alerts.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        routines.root.visibility = View.GONE
        content.root.visibility = View.GONE
        myTeo.close()
        agenda.showList()
        agenda.root.visibility = View.VISIBLE
        bottom.visibility = View.VISIBLE
        updatePreviewBanner(homeVisible = false, agendaVisible = true)
    }

    private fun renderHomeState() {
        val scene = HomePreviewStates.snapshot(selectedHomeState)
        val absent = getString(R.string.home_absent)
        HomeScreenRenderer.status(home, scene.status, waiting = false)
        HomeScreenRenderer.interactions(home, scene.interactions ?: absent)
        HomeScreenRenderer.signal(home, scene.signal ?: absent, known = scene.signal != null)
        HomeScreenRenderer.playback(home, scene.playback, waiting = false)
        HomeScreenRenderer.notice(home, scene.notice)
    }

    private fun openMyTeo(connection: Boolean) {
        val bottom = findViewById<BottomNavigationView>(R.id.preview_bottom_nav)
        myTeoOrigin = bottom.selectedItemId
        hideSections()
        if (connection) myTeo.openConnection() else myTeo.openMenu()
        bottom.visibility = View.GONE
        updatePreviewBanner(homeVisible = false, myTeoVisible = true)
    }

    private fun showOrigin(bottom: BottomNavigationView) {
        when (myTeoOrigin) {
            R.id.nav_routines -> presentRoutines()
            R.id.nav_stories -> presentContent()
            R.id.nav_medical -> presentAgenda()
            else -> presentHome()
        }
        bottom.visibility = View.VISIBLE
    }

    private fun hideSections() {
        home.root.visibility = View.GONE
        alerts.root.visibility = View.GONE
        activityPreview.root.visibility = View.GONE
        routines.root.visibility = View.GONE
        content.root.visibility = View.GONE
        agenda.root.visibility = View.GONE
    }

    private fun updatePreviewBanner(
        homeVisible: Boolean,
        contentVisible: Boolean = false,
        agendaVisible: Boolean = false,
        myTeoVisible: Boolean = false,
        activityVisible: Boolean = false,
        alertsVisible: Boolean = false,
    ) {
        val banner = findViewById<MaterialButton>(R.id.txt_example_banner)
        banner.isClickable = homeVisible || contentVisible || agendaVisible || myTeoVisible ||
            activityVisible || alertsVisible
        banner.isFocusable = banner.isClickable
        if (activityVisible) {
            banner.text = getString(R.string.preview_activity_banner, activityPreview.scenarioLabel)
            banner.contentDescription = banner.text
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseActivityScenario() }
            return
        }
        if (alertsVisible) {
            banner.text = getString(R.string.preview_alerts_banner, alertsPreview.scenarioLabel)
            banner.contentDescription = banner.text
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseAlertsScenario() }
            return
        }
        if (myTeoVisible) {
            banner.text = getString(R.string.preview_my_teo_banner, myTeo.scenarioLabel)
            banner.contentDescription = banner.text
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseMyTeoScenario() }
            return
        }
        if (agendaVisible) {
            banner.text = getString(R.string.preview_agenda_banner, agenda.scenarioLabel)
            banner.contentDescription = banner.text
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseAgendaScenario() }
            return
        }
        if (contentVisible) {
            banner.text = getString(R.string.preview_content_banner, content.scenarioLabel)
            banner.contentDescription = banner.text
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseContentScenario() }
            return
        }
        if (homeVisible) {
            val label = getString(selectedHomeState.labelRes)
            banner.text = getString(R.string.preview_home_state_banner, label)
            banner.contentDescription = getString(R.string.preview_home_state_description, label)
            banner.setIconResource(R.drawable.teo_preview_expand)
            banner.setOnClickListener { chooseHomeState() }
        } else {
            banner.setText(R.string.preview_example_banner)
            banner.contentDescription = null
            banner.icon = null
            banner.setOnClickListener(null)
            banner.isClickable = false
        }
    }

    private fun chooseHomeState() {
        val states = HomePreviewState.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_home_state_title)
            .setSingleChoiceItems(labels, states.indexOf(selectedHomeState)) { dialog, index ->
                selectedHomeState = states[index]
                renderHomeState()
                updatePreviewBanner(homeVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    private fun chooseContentScenario() {
        val states = ContentPreviewScenario.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_example_banner)
            .setSingleChoiceItems(labels, states.indexOfFirst { getString(it.labelRes) == content.scenarioLabel }) { dialog, index ->
                content.chooseScenario(states[index])
                updatePreviewBanner(homeVisible = false, contentVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    private fun chooseMyTeoScenario() {
        val states = MyTeoPreviewChoice.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_example_banner)
            .setSingleChoiceItems(labels, states.indexOfFirst { getString(it.labelRes) == myTeo.scenarioLabel }) { dialog, index ->
                myTeo.choose(states[index])
                updatePreviewBanner(homeVisible = false, myTeoVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    private fun chooseAgendaScenario() {
        val states = AgendaPreviewScenario.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_example_banner)
            .setSingleChoiceItems(labels, states.indexOfFirst { getString(it.labelRes) == agenda.scenarioLabel }) { dialog, index ->
                agenda.chooseScenario(states[index])
                updatePreviewBanner(homeVisible = false, agendaVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    private fun chooseActivityScenario() {
        val states = ActivityPreviewScenario.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_example_banner)
            .setSingleChoiceItems(labels, states.indexOf(activityPreview.scenario)) { dialog, index ->
                activityPreview.choose(states[index])
                updatePreviewBanner(homeVisible = false, activityVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    private fun chooseAlertsScenario() {
        val states = AlertsPreviewScenario.entries
        val labels = states.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.preview_example_banner)
            .setSingleChoiceItems(labels, states.indexOf(alertsPreview.scenario)) { dialog, index ->
                alertsPreview.choose(states[index])
                updatePreviewBanner(homeVisible = false, alertsVisible = true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.preview_home_state_close, null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(HOME_STATE_KEY, selectedHomeState.name)
        outState.putString(ACTIVITY_STATE_KEY, activityPreview.scenario.name)
        outState.putInt(ACTIVITY_DAYS_KEY, activityPreview.days)
        outState.putString(ALERTS_STATE_KEY, alertsPreview.scenario.name)
        content.persist(outState)
        agenda.persist(outState)
        myTeo.persist(outState)
        super.onSaveInstanceState(outState)
    }

    private fun stackPlayback() {
        HomeScreenRenderer.stack(
            home,
            resources.configuration.fontScale,
            resources.configuration.screenWidthDp,
        )
    }

    private fun simulated(anchor: View) {
        Snackbar.make(anchor, R.string.preview_simulated, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val HOME_STATE_KEY = "preview_home_state"
        private const val ACTIVITY_STATE_KEY = "preview_activity_state"
        private const val ACTIVITY_DAYS_KEY = "preview_activity_days"
        private const val ALERTS_STATE_KEY = "preview_alerts_state"
    }
}

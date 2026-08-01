package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.player.local.mpv.MPVPlayer
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject

/**
 * Bottom sheet dialog for customizing ASS/SSA subtitle appearance in real time.
 *
 * Changes are applied immediately to [MPVPlayer] via [MPVPlayer.applySubtitleSettings]
 * and also persisted in [AppPreferences] so they survive across sessions.
 *
 * The [overrideMode] controls whether mpv respects the ASS file's embedded styles:
 *   "no"    = fully respect file styles (default, safe for all content)
 *   "scale" = only apply scale override, keep other ASS styles
 *   "yes"   = apply user overrides on top of file styles
 *   "force" = aggressively ignore all embedded styles
 */
@AndroidEntryPoint
class SubtitleSettingsDialogFragment(
    private val viewModel: PlayerViewModel,
) : BottomSheetDialogFragment() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.dialog_subtitle_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Override Mode dropdown ─────────────────────────────────────────
        val overrideModeOptions = listOf("no", "scale", "yes", "force")
        val overrideModeLabels = listOf(
            getString(R.string.subtitle_override_no),
            getString(R.string.subtitle_override_scale),
            getString(R.string.subtitle_override_yes),
            getString(R.string.subtitle_override_force),
        )
        val overrideModeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            overrideModeLabels,
        )
        val overrideModeDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.dropdown_override_mode)
        overrideModeDropdown.setAdapter(overrideModeAdapter)

        val currentOverride = appPreferences.getValue(appPreferences.playerMpvSubAssOverride)
        val currentOverrideLabel =
            overrideModeLabels.getOrNull(overrideModeOptions.indexOf(currentOverride))
                ?: overrideModeLabels[0]
        overrideModeDropdown.setText(currentOverrideLabel, false)

        // ── Scale slider (50% – 200%) ──────────────────────────────────────
        val scaleSlider = view.findViewById<Slider>(R.id.slider_scale)
        val scaleLabel = view.findViewById<TextView>(R.id.label_scale_value)
        val currentScale = appPreferences.getValue(appPreferences.playerMpvSubScale)
        scaleSlider.value = currentScale.toFloat().coerceIn(50f, 200f)
        scaleLabel.text = "${currentScale}%"
        scaleSlider.addOnChangeListener { _, value, _ ->
            scaleLabel.text = "${value.toInt()}%"
        }

        // ── Font Size input ────────────────────────────────────────────────
        val fontSizeInput = view.findViewById<TextInputEditText>(R.id.input_font_size)
        val currentFontSize = appPreferences.getValue(appPreferences.playerMpvSubFontSize)
        if (currentFontSize > 0) fontSizeInput.setText(currentFontSize.toString())

        // ── Font name input ────────────────────────────────────────────────
        val fontInput = view.findViewById<TextInputEditText>(R.id.input_font)
        fontInput.setText(appPreferences.getValue(appPreferences.playerMpvSubFont))

        // ── Text color input ───────────────────────────────────────────────
        val colorInput = view.findViewById<TextInputEditText>(R.id.input_color)
        colorInput.setText(appPreferences.getValue(appPreferences.playerMpvSubColor))

        // ── Border size slider (0 – 10) ────────────────────────────────────
        val borderSlider = view.findViewById<Slider>(R.id.slider_border_size)
        val borderLabel = view.findViewById<TextView>(R.id.label_border_value)
        val currentBorder = appPreferences.getValue(appPreferences.playerMpvSubBorderSize)
        borderSlider.value = (if (currentBorder < 0) 2f else currentBorder.toFloat()).coerceIn(0f, 10f)
        borderLabel.text = borderSlider.value.toInt().toString()
        borderSlider.addOnChangeListener { _, value, _ ->
            borderLabel.text = value.toInt().toString()
        }

        // ── Border color input ─────────────────────────────────────────────
        val borderColorInput = view.findViewById<TextInputEditText>(R.id.input_border_color)
        borderColorInput.setText(appPreferences.getValue(appPreferences.playerMpvSubBorderColor))

        // ── Shadow offset slider (0 – 10) ──────────────────────────────────
        val shadowSlider = view.findViewById<Slider>(R.id.slider_shadow_offset)
        val shadowLabel = view.findViewById<TextView>(R.id.label_shadow_value)
        val currentShadow = appPreferences.getValue(appPreferences.playerMpvSubShadowOffset)
        shadowSlider.value = (if (currentShadow < 0) 2f else currentShadow.toFloat()).coerceIn(0f, 10f)
        shadowLabel.text = shadowSlider.value.toInt().toString()
        shadowSlider.addOnChangeListener { _, value, _ ->
            shadowLabel.text = value.toInt().toString()
        }

        // ── Margin Y slider (0 – 200px) ────────────────────────────────────
        val marginSlider = view.findViewById<Slider>(R.id.slider_margin_y)
        val marginLabel = view.findViewById<TextView>(R.id.label_margin_value)
        val currentMargin = appPreferences.getValue(appPreferences.playerMpvSubMarginY)
        marginSlider.value = (if (currentMargin < 0) 36f else currentMargin.toFloat()).coerceIn(0f, 200f)
        marginLabel.text = "${marginSlider.value.toInt()}px"
        marginSlider.addOnChangeListener { _, value, _ ->
            marginLabel.text = "${value.toInt()}px"
        }

        // ── Reset button ───────────────────────────────────────────────────
        view.findViewById<Button>(R.id.btn_subtitle_reset).setOnClickListener {
            scaleSlider.value = 100f
            fontSizeInput.setText("")
            fontInput.setText("")
            colorInput.setText("")
            borderSlider.value = 2f
            borderColorInput.setText("")
            shadowSlider.value = 2f
            marginSlider.value = 36f
            overrideModeDropdown.setText(overrideModeLabels[0], false)
        }

        // ── Apply / Close buttons ──────────────────────────────────────────
        view.findViewById<Button>(R.id.btn_subtitle_apply).setOnClickListener {
            applyAndSave(
                overrideModeOptions = overrideModeOptions,
                overrideModeLabels = overrideModeLabels,
                overrideModeDropdown = overrideModeDropdown,
                scaleSlider = scaleSlider,
                fontInput = fontInput,
                fontSizeInput = fontSizeInput,
                colorInput = colorInput,
                borderSlider = borderSlider,
                borderColorInput = borderColorInput,
                shadowSlider = shadowSlider,
                marginSlider = marginSlider,
            )
        }

        view.findViewById<Button>(R.id.btn_subtitle_close).setOnClickListener {
            dismiss()
        }
    }

    private fun applyAndSave(
        overrideModeOptions: List<String>,
        overrideModeLabels: List<String>,
        overrideModeDropdown: AutoCompleteTextView,
        scaleSlider: Slider,
        fontInput: TextInputEditText,
        fontSizeInput: TextInputEditText,
        colorInput: TextInputEditText,
        borderSlider: Slider,
        borderColorInput: TextInputEditText,
        shadowSlider: Slider,
        marginSlider: Slider,
    ) {
        val selectedLabel = overrideModeDropdown.text.toString()
        val overrideMode = overrideModeOptions.getOrNull(overrideModeLabels.indexOf(selectedLabel))
            ?: "no"

        val scale = scaleSlider.value.toInt()
        val font = fontInput.text?.toString()?.trim() ?: ""
        val fontSize = fontSizeInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val color = colorInput.text?.toString()?.trim() ?: ""
        val borderSize = borderSlider.value.toInt()
        val borderColor = borderColorInput.text?.toString()?.trim() ?: ""
        val shadowOffset = shadowSlider.value.toInt()
        val marginY = marginSlider.value.toInt()

        // Persist preferences
        appPreferences.setValue(appPreferences.playerMpvSubAssOverride, overrideMode)
        appPreferences.setValue(appPreferences.playerMpvSubScale, scale)
        appPreferences.setValue(appPreferences.playerMpvSubFont, font)
        appPreferences.setValue(appPreferences.playerMpvSubFontSize, fontSize)
        appPreferences.setValue(appPreferences.playerMpvSubColor, color)
        appPreferences.setValue(appPreferences.playerMpvSubBorderSize, borderSize)
        appPreferences.setValue(appPreferences.playerMpvSubBorderColor, borderColor)
        appPreferences.setValue(appPreferences.playerMpvSubShadowOffset, shadowOffset)
        appPreferences.setValue(appPreferences.playerMpvSubMarginY, marginY)

        // Apply immediately to the running MPV instance
        val mpvPlayer = viewModel.player as? MPVPlayer
        mpvPlayer?.applySubtitleSettings(
            overrideMode = overrideMode,
            font = font,
            fontSize = fontSize,
            color = color,
            borderSize = borderSize,
            borderColor = borderColor,
            shadowOffset = shadowOffset,
            marginY = marginY,
            scale = scale,
        )

        dismiss()
    }
}

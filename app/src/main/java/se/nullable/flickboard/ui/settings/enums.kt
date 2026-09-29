package se.nullable.flickboard.ui.settings

import androidx.annotation.StringRes
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.model.layouts.AR_MESSAGEASE
import se.nullable.flickboard.model.layouts.BG_HLONRNO
import se.nullable.flickboard.model.layouts.BG_THUMBKEY
import se.nullable.flickboard.model.layouts.DA_MESSAGEASE
import se.nullable.flickboard.model.layouts.DE_EO_MESSAGEASE
import se.nullable.flickboard.model.layouts.DE_MESSAGEASE
import se.nullable.flickboard.model.layouts.EN_DE_MESSAGEASE
import se.nullable.flickboard.model.layouts.EN_ES_MESSAGEASE
import se.nullable.flickboard.model.layouts.EN_MESSAGEASE
import se.nullable.flickboard.model.layouts.EN_RO_THUMBKEY
import se.nullable.flickboard.model.layouts.EN_THUMBKEY
import se.nullable.flickboard.model.layouts.EO_MESSAGEASE
import se.nullable.flickboard.model.layouts.ES_CA_MESSAGEASE
import se.nullable.flickboard.model.layouts.ES_MESSAGEASE
import se.nullable.flickboard.model.layouts.FA_MESSAGEASE
import se.nullable.flickboard.model.layouts.FA_THUMBKEY
import se.nullable.flickboard.model.layouts.FR_EN_FRAPPEFLUIDE
import se.nullable.flickboard.model.layouts.FR_EXT_MESSAGEASE
import se.nullable.flickboard.model.layouts.FR_MESSAGEASE
import se.nullable.flickboard.model.layouts.FR_PUNC_MESSAGEASE
import se.nullable.flickboard.model.layouts.HEB_MESSAGEASE
import se.nullable.flickboard.model.layouts.HU_DT_MESSAGEASE
import se.nullable.flickboard.model.layouts.HU_MESSAGEASE
import se.nullable.flickboard.model.layouts.HU_MF_MESSAGEASE
import se.nullable.flickboard.model.layouts.HU_UUP_MESSAGEASE
import se.nullable.flickboard.model.layouts.IT_MESSAGEASE
import se.nullable.flickboard.model.layouts.KA_SOLOVEV
import se.nullable.flickboard.model.layouts.NO_THUMBKEY
import se.nullable.flickboard.model.layouts.PL_RMITURA_MESSAGEASE
import se.nullable.flickboard.model.layouts.PT_IOS_MESSAGEASE
import se.nullable.flickboard.model.layouts.PT_MESSAGEASE
import se.nullable.flickboard.model.layouts.RU_MESSAGEASE
import se.nullable.flickboard.model.layouts.RU_PHONETIC_MESSAGEASE
import se.nullable.flickboard.model.layouts.SV_DE_MESSAGEASE
import se.nullable.flickboard.model.layouts.SV_MESSAGEASE
import se.nullable.flickboard.model.layouts.TR_MESSAGEASE
import se.nullable.flickboard.model.layouts.UK_MESSAGEASE
import se.nullable.flickboard.model.layouts.UK_RU_MESSAGEASE
import se.nullable.flickboard.model.layouts.messageaseNumericCalculatorLayer
import se.nullable.flickboard.model.layouts.messageaseNumericPhoneLayer
import se.nullable.flickboard.model.layouts.miniNumbersCalculatorLayer
import se.nullable.flickboard.model.layouts.miniNumbersPhoneLayer

enum class EnabledLayers(
    @param:StringRes override val label: Int,
    val type: LayoutType,
    val hasNumbers: Boolean = false,
) : Labeled {
    All(R.string.enabledLayers_all, LayoutType.Dual, hasNumbers = true),
    Letters(R.string.enabledLayers_lettersOnly, LayoutType.Single, hasNumbers = false),
    Numbers(R.string.enabledLayers_numbersOnly, LayoutType.Single, hasNumbers = true),
    DoubleLetters(R.string.enabledLayers_doubleLetters, LayoutType.Dual, hasNumbers = false),
    AllMiniNumbers(
        R.string.enabledLayers_allMiniNumbers,
        LayoutType.MiniNumbers,
        hasNumbers = true,
    ),
    AllMiniNumbersMiddle(
        R.string.enabledLayers_allMiniNumbersInMiddle, LayoutType.MiniNumbers,
        hasNumbers = true,
    ),
    AllMiniNumbersOpposite(
        R.string.enabledLayers_allMiniNumbersOnOppositeSide, LayoutType.MiniNumbers,
        hasNumbers = true,
    );

    val toggleNumbers: EnabledLayers?
        get() = when (this) {
            Letters -> Numbers
            Numbers -> Letters
            DoubleLetters -> All
            All -> DoubleLetters
            else -> null
        }
}

enum class LayoutType {
    Single,
    Dual,
    MiniNumbers,
}

sealed interface EnabledLayersLandscape : Labeled {
    data class Set(val setting: EnabledLayers) : EnabledLayersLandscape {
        @get:StringRes
        override val label: Int = setting.label
        override fun toString(): String = setting.toString()
    }

    data object Inherit : EnabledLayersLandscape {
        @get:StringRes
        override val label: Int = R.string.enabledLayers_landscape_inherit
    }

    companion object {
        val entries = listOf(Inherit) + EnabledLayers.entries.map(EnabledLayersLandscape::Set)
        fun tryEnumValueOf(str: String): EnabledLayersLandscape? = when (str) {
            "Inherit" -> Inherit
            else -> se.nullable.flickboard.util.tryEnumValueOf<EnabledLayers>(str)
                ?.let(EnabledLayersLandscape::Set)
        }
    }
}

enum class Handedness(@param:StringRes override val label: Int) : Labeled {
    LeftHanded(R.string.handedness_leftHanded),
    RightHanded(R.string.handedness_rightHanded);

    operator fun not(): Handedness = when (this) {
        LeftHanded -> RightHanded
        RightHanded -> LeftHanded
    }
}

enum class InvertSwipeDelete(
    @param:StringRes override val label: Int,
    @param:StringRes val description: Int,
    val forHandedness: (Handedness) -> Boolean
) : Labeled {
    Never(
        R.string.invertSwipeDelete_never,
        description = R.string.invertSwipeDelete_never_desc,
        forHandedness = { false },
    ),
    Always(
        R.string.invertSwipeDelete_always,
        description = R.string.invertSwipeDelete_always_desc,
        forHandedness = { true },
    ),
    RightHanded(
        R.string.handedness_rightHanded,
        description = R.string.invertSwipeDelete_rightHanded_desc,
        forHandedness = { it == Handedness.RightHanded },
    ),
    LeftHanded(
        R.string.handedness_leftHanded,
        description = R.string.invertSwipeDelete_leftHanded_desc,
        forHandedness = { it == Handedness.LeftHanded },
    ),
}

enum class LetterLayerOption(@param:StringRes override val label: Int, val layout: Layout) :
    Labeled {
    Arabic(R.string.letterLayerOption_arabic, AR_MESSAGEASE),
    BulgarianHlonrno(R.string.letterLayerOption_bulgarianHlonrno, BG_HLONRNO),
    BulgarianThumbKey(R.string.letterLayerOption_bulgarianThumbKey, BG_THUMBKEY),
    Danish(R.string.letterLayerOption_danish, DA_MESSAGEASE),
    English(R.string.letterLayerOption_english, EN_MESSAGEASE),
    EnglishThumbKey(R.string.letterLayerOption_englishThumbKey, EN_THUMBKEY),
    Esperanto(R.string.letterLayerOption_esperanto, EO_MESSAGEASE),
    French(R.string.letterLayerOption_french, FR_MESSAGEASE),
    FrenchExt(R.string.letterLayerOption_frenchExt, FR_EXT_MESSAGEASE),
    FrenchPunc(R.string.letterLayerOption_frenchPunc, FR_PUNC_MESSAGEASE),
    FrenchEnglishFrappeFluide(
        R.string.letterLayerOption_frenchEnglishFrappefluide,
        FR_EN_FRAPPEFLUIDE,
    ),
    German(R.string.letterLayerOption_german, DE_MESSAGEASE),
    GermanEnglish(R.string.letterLayerOption_germanEnglish, EN_DE_MESSAGEASE),
    GermanEsperanto(R.string.letterLayerOption_germanEsperanto, DE_EO_MESSAGEASE),
    Georgian(R.string.letterLayerOption_georgian, KA_SOLOVEV),
    Hebrew(R.string.letterLayerOption_hebrew, HEB_MESSAGEASE),
    Hungarian(R.string.letterLayerOption_hungarian, HU_MESSAGEASE),
    HungarianDT(R.string.letterLayerOption_hungarianDt, HU_DT_MESSAGEASE),
    HungarianMF(R.string.letterLayerOption_hungarianMf, HU_MF_MESSAGEASE),
    HungarianUUp(R.string.letterLayerOption_hungarianUUp, HU_UUP_MESSAGEASE),
    Italian(R.string.letterLayerOption_italian, IT_MESSAGEASE),
    NorwegianThumbKey(R.string.letterLayerOption_norwegianThumbKey, NO_THUMBKEY),
    Persian(R.string.letterLayerOption_persian, FA_MESSAGEASE),
    PersianThumbKey(R.string.letterLayerOption_persianThumbKey, FA_THUMBKEY),
    PolishRmitura(R.string.letterLayerOption_polishRmitura, PL_RMITURA_MESSAGEASE),
    Portuguese(R.string.letterLayerOption_portuguese, PT_MESSAGEASE),
    PortugueseIos(R.string.letterLayerOption_portugueseIos, PT_IOS_MESSAGEASE),
    RomanianEnglishThumbKey(R.string.letterLayerOption_romanianEnglishThumbKey, EN_RO_THUMBKEY),
    Russian(R.string.letterLayerOption_russian, RU_MESSAGEASE),
    RussianPhonetic(R.string.letterLayerOption_russianPhonetic, RU_PHONETIC_MESSAGEASE),
    Spanish(R.string.letterLayerOption_spanish, ES_MESSAGEASE),
    SpanishCatalan(R.string.letterLayerOption_spanishCatalan, ES_CA_MESSAGEASE),
    SpanishEnglish(R.string.letterLayerOption_spanishEnglish, EN_ES_MESSAGEASE),
    Swedish(R.string.letterLayerOption_swedish, SV_MESSAGEASE),
    SwedishDE(R.string.letterLayerOption_swedishDe, SV_DE_MESSAGEASE),
    Turkish(R.string.letterLayerOption_turkish, TR_MESSAGEASE),
    Ukrainian(R.string.letterLayerOption_ukrainian, UK_MESSAGEASE),
    UkrainianRussian(R.string.letterLayerOption_ukrainianRussian, UK_RU_MESSAGEASE),
}

enum class NumericLayerOption(
    @param:StringRes override val label: Int,
    val fullSizedLayer: (Layout) -> Layer,
    val miniLayer: (Layout) -> Layer
) : Labeled {
    Phone(
        R.string.numericLayerOption_phone,
        fullSizedLayer = { messageaseNumericPhoneLayer(it.digits) },
        miniLayer = { miniNumbersPhoneLayer(it.digits) },
    ),
    Calculator(
        R.string.numericLayerOption_calculator,
        fullSizedLayer = { messageaseNumericCalculatorLayer(it.digits) },
        miniLayer = { miniNumbersCalculatorLayer(it.digits) },
    ),
}

enum class ControlSectionOption(@param:StringRes override val label: Int) : Labeled {
    Single(R.string.controlSectionOption_single),
    DoubleInside(R.string.controlSectionOption_doubleInside),
    DoubleOutside(R.string.controlSectionOption_doubleOutside),
}

enum class ModifierBehaviourOption(@param:StringRes override val label: Int) : Labeled {
    Digit(R.string.modifierBehaviourOption_digit),
    Shift(R.string.modifierBehaviourOption_shift)
}


enum class GestureRecognizer(
    @param:StringRes override val label: Int,
    @param:StringRes val description: Int
) : Labeled {
    Default(
        R.string.gestureRecognizer_default,
        description = R.string.gestureRecognizer_default_desc,
    ),
    Dollar1(
        R.string.gestureRecognizer_dollar1,
        description = R.string.gestureRecognizer_dollar1_desc,
    ),
}

enum class PointerTrailRenderer(
    @param:StringRes override val label: Int,
) : Labeled {
    Deferred(R.string.pointerTrailRenderer_deferred),
    Eager(R.string.pointerTrailRenderer_eager),
}
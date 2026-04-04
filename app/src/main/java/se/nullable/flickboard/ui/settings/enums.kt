package se.nullable.flickboard.ui.settings

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
    override val label: String,
    val type: LayoutType,
    val hasNumbers: Boolean = false,
) : Labeled {
    All("All", LayoutType.Dual, hasNumbers = true),
    Letters("Letters only", LayoutType.Single, hasNumbers = false),
    Numbers("Numbers only", LayoutType.Single, hasNumbers = true),
    DoubleLetters("Double letters", LayoutType.Dual, hasNumbers = false),
    AllMiniNumbers("All (mini numbers)", LayoutType.MiniNumbers, hasNumbers = true),
    AllMiniNumbersMiddle(
        "All (mini numbers in middle)", LayoutType.MiniNumbers,
        hasNumbers = true,
    ),
    AllMiniNumbersOpposite(
        "All (mini numbers on opposite side)", LayoutType.MiniNumbers,
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
        override val label: String = setting.label
        override fun toString(): String = setting.toString()
    }

    data object Inherit : EnabledLayersLandscape {
        override val label: String = "Same as portrait"
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

enum class Handedness(override val label: String) : Labeled {
    LeftHanded("Left-handed"),
    RightHanded("Right-handed");

    operator fun not(): Handedness = when (this) {
        LeftHanded -> RightHanded
        RightHanded -> LeftHanded
    }
}

enum class InvertSwipeDelete(
    override val label: String,
    val description: String,
    val forHandedness: (Handedness) -> Boolean
) : Labeled {
    Never(
        "Never", description = "Never invert the delete direction.",
        forHandedness = { false },
    ),
    Always(
        "Always", description = "Always invert the swipe-to-delete direction.",
        forHandedness = { true },
    ),
    RightHanded(
        "Righthanded",
        description = "Only invert the swipe-to-delete direction when in right-handed mode (where the delete key will usually be on the left edge of the screen).",
        forHandedness = { it == Handedness.RightHanded },
    ),
    LeftHanded(
        "Lefthanded",
        "Only invert the swipe-to-delete direction when in left-handed mode (where the delete key will usually be on the right edge of the screen).",
        forHandedness = { it == Handedness.LeftHanded },
    ),
}

enum class LetterLayerOption(override val label: String, val layout: Layout) : Labeled {
    Arabic("Arabic (MessagEase)", AR_MESSAGEASE),
    BulgarianHlonrno("Bulgarian (hlonrno)", BG_HLONRNO),
    BulgarianThumbKey("Bulgarian (Thumb-Key)", BG_THUMBKEY),
    Danish("Danish (MessagEase)", DA_MESSAGEASE),
    English("English (MessagEase)", EN_MESSAGEASE),
    EnglishThumbKey("English (Thumb-Key)", EN_THUMBKEY),
    Esperanto("Esperanto (MessagEase)", EO_MESSAGEASE),
    French("French (MessagEase)", FR_MESSAGEASE),
    FrenchExt("French (Extended MessagEase)", FR_EXT_MESSAGEASE),
    FrenchPunc("French (Punctuation MessagEase)", FR_PUNC_MESSAGEASE),
    FrenchEnglishFrappeFluide("French & English (Frappefluide)", FR_EN_FRAPPEFLUIDE),
    German("German (MessagEase)", DE_MESSAGEASE),
    GermanEnglish("German & English (MessagEase-style)", EN_DE_MESSAGEASE),
    GermanEsperanto("German & Esperanto (MessagEase-style)", DE_EO_MESSAGEASE),
    Georgian("Georgian (by Vladimir Solovev)", KA_SOLOVEV),
    Hebrew("Hebrew (MessagEase)", HEB_MESSAGEASE),
    Hungarian("Hungarian (MessagEase)", HU_MESSAGEASE),
    HungarianDT("Hungarian (MessagEase-style, by Dániel Tenke)", HU_DT_MESSAGEASE),
    HungarianMF("Hungarian (MessagEase-style, by Máté Farkas)", HU_MF_MESSAGEASE),
    HungarianUUp("Hungarian (MessagEase-style, U always up)", HU_UUP_MESSAGEASE),
    Italian("Italian (MessagEase)", IT_MESSAGEASE),
    NorwegianThumbKey("Norwegian (Thumb-Key)", NO_THUMBKEY),
    Persian("Persian (MessagEase)", FA_MESSAGEASE),
    PersianThumbKey("Persian (Thumb-Key)", FA_THUMBKEY),
    PolishRmitura("PL (MessagEase-style, Rmitura)", PL_RMITURA_MESSAGEASE),
    Portuguese("Portuguese (MessagEase)", PT_MESSAGEASE),
    PortugueseIos("Portuguese (MessagEase, iOS)", PT_IOS_MESSAGEASE),
    Russian("Russian (MessagEase)", RU_MESSAGEASE),
    RussianPhonetic("Russian phonetic (MessagEase)", RU_PHONETIC_MESSAGEASE),
    Spanish("Spanish (MessagEase)", ES_MESSAGEASE),
    SpanishCatalan("Spanish & Catalan (MessagEase-style)", ES_CA_MESSAGEASE),
    SpanishEnglish("Spanish & English (MessagEase)", EN_ES_MESSAGEASE),
    Swedish("Swedish (MessagEase-style)", SV_MESSAGEASE),
    SwedishDE("Swedish (MessagEase, German-style)", SV_DE_MESSAGEASE),
    Turkish("Turkish (MessagEase)", TR_MESSAGEASE),
    Ukrainian("Ukrainian (MessagEase)", UK_MESSAGEASE),
    UkrainianRussian("Ukrainian Russian (MessagEase)", UK_RU_MESSAGEASE),
}

enum class NumericLayerOption(
    override val label: String,
    val fullSizedLayer: (Layout) -> Layer,
    val miniLayer: (Layout) -> Layer
) : Labeled {
    Phone(
        "Phone",
        fullSizedLayer = { messageaseNumericPhoneLayer(it.digits) },
        miniLayer = { miniNumbersPhoneLayer(it.digits) },
    ),
    Calculator(
        "Calculator",
        fullSizedLayer = { messageaseNumericCalculatorLayer(it.digits) },
        miniLayer = { miniNumbersCalculatorLayer(it.digits) },
    ),
}

enum class ControlSectionOption(override val label: String) : Labeled {
    Single("Single"),
    DoubleInside("Double (inside)"),
    DoubleOutside("Double (outside)"),
}

enum class ModifierBehaviourOption(override val label: String) : Labeled {
    Digit("Type digit"),
    Shift("Use shifted action")
}


enum class GestureRecognizer(override val label: String, val description: String) : Labeled {
    Default("Default", description = "The default FlickBoard gesture recognizer"),
    Dollar1(
        "$1 (OLD EXPERIMENT)",
        description = "Experimental legacy gesture recognizer (many recognition settings do not apply)",
    ),
}
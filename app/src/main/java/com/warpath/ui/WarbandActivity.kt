package com.warpath.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.warpath.R
import com.warpath.model.UnitCategory
import com.warpath.model.UnitType

class WarbandActivity : AppCompatActivity() {

    private lateinit var squadList: LinearLayout
    private lateinit var suppliesText: TextView
    private lateinit var slotsText: TextView
    private lateinit var upgradeSection: LinearLayout
    private lateinit var feedbackBanner: TextView
    private val gameState get() = CampaignActivity.campaignManager.gameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val canRecruit = intent.getBooleanExtra("can_recruit", false)

        val frame = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(UiTheme.BASE_BG)) }

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5))
        }

        // Title row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val backArrow = TextView(this).apply {
            text = "‹"
            textSize = UiTheme.TEXT_HEADER
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            setPadding(0, 0, dp(UiTheme.SPACE_3), 0)
            setOnClickListener { finish() }
        }
        titleRow.addView(backArrow)
        titleRow.addView(TextView(this).apply {
            text = "YOUR SQUADRON"
            textSize = UiTheme.TEXT_SECTION
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = UiTheme.TYPEFACE_TITLE
            letterSpacing = 0.06f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layout.addView(titleRow, marginParams(bottom = UiTheme.SPACE_2))

        // Resource row
        val resourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiTheme.roundedRect(UiTheme.SURFACE_ALT, UiTheme.BORDER, UiTheme.RADIUS_SM)
            setPadding(dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_2))
        }
        suppliesText = TextView(this).apply {
            textSize = UiTheme.TEXT_SECONDARY
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
        }
        resourceRow.addView(suppliesText)
        layout.addView(resourceRow, marginParams(bottom = UiTheme.SPACE_4))

        // Slots header
        slotsText = TextView(this).apply {
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.04f
        }
        layout.addView(slotsText, marginParams(bottom = UiTheme.SPACE_3))

        squadList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(squadList, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Recruit section
        if (canRecruit) {
            val recruitSection = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Divider
            recruitSection.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(UiTheme.DIVIDER))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(UiTheme.SPACE_4); bottomMargin = dp(UiTheme.SPACE_4)
            })

            recruitSection.addView(TextView(this).apply {
                text = "SCRAMBLE"
                textSize = UiTheme.TEXT_SECONDARY
                setTextColor(Color.parseColor(UiTheme.GOLD))
                typeface = UiTheme.TYPEFACE_LABEL
                letterSpacing = 0.08f
            }, marginParams(bottom = UiTheme.SPACE_3))

            val recruitableUnits = listOf(
                Triple(UnitType.MILITIA_SPEAR, 6, 30),
                Triple(UnitType.SHIELD_INFANTRY, 4, 40),
                Triple(UnitType.JAVELIN_SKIRMISHER, 5, 35),
                Triple(UnitType.ARCHER, 4, 35),
                Triple(UnitType.LIGHT_CAVALRY, 3, 45),
                Triple(UnitType.BANNER_BEARER, 2, 40),
                Triple(UnitType.FIELD_MEDIC, 2, 35)
            )

            for ((unitType, count, cost) in recruitableUnits) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = UiTheme.rippleDrawable(UiTheme.SURFACE_ALT, UiTheme.BORDER, UiTheme.RADIUS_SM)
                    setPadding(dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_3))
                    setOnClickListener { attemptRecruit(unitType, count, cost) }
                }
                val catDot = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(getCategoryColor(unitType.category))
                    }
                }
                row.addView(catDot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(UiTheme.SPACE_3) })
                row.addView(TextView(this).apply {
                    text = "${unitType.name} x$count"
                    textSize = UiTheme.TEXT_BODY_SM
                    typeface = UiTheme.TYPEFACE_BODY
                    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(TextView(this).apply {
                    text = "$cost fuel"
                    textSize = UiTheme.TEXT_SECONDARY
                    typeface = UiTheme.TYPEFACE_LABEL
                    setTextColor(Color.parseColor(UiTheme.WARNING))
                })
                recruitSection.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(UiTheme.SPACE_2) })
            }
            layout.addView(recruitSection)
        }

        // Upgrade section
        upgradeSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(upgradeSection)
        rebuildUpgradeSection()

        scroll.addView(layout)
        frame.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Feedback banner (replaces Toast)
        feedbackBanner = TextView(this).apply {
            textSize = UiTheme.TEXT_SECONDARY
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            gravity = Gravity.CENTER
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
            background = UiTheme.roundedRect(UiTheme.SURFACE_ELEVATED, UiTheme.BORDER, UiTheme.RADIUS_MD)
            elevation = dpF(UiTheme.SHEET_ELEVATION)
            visibility = View.GONE
        }
        frame.addView(feedbackBanner, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = dp(UiTheme.SPACE_6) })

        setContentView(frame)
        refreshUI()
    }

    private fun attemptRecruit(unitType: UnitType, count: Int, cost: Int) {
        if (CampaignActivity.campaignManager.recruitUnit(unitType, count, cost)) {
            showFeedback("Scrambled ${unitType.name}")
            refreshUI()
        } else if (gameState.supplies < cost) {
            showFeedback("Need $cost fuel")
        } else {
            showFeedback("Squadron is full")
        }
    }

    private fun rebuildUpgradeSection() {
        upgradeSection.removeAllViews()
        if (gameState.maxWarbandSlots < 6) {
            upgradeSection.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(UiTheme.DIVIDER))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(UiTheme.SPACE_4); bottomMargin = dp(UiTheme.SPACE_4)
            })
            val slotCost = gameState.maxWarbandSlots * 40
            val upgradeBtn = Button(this).apply {
                text = "Expand Squadron (+1 slot) · $slotCost fuel"
                applyPrimaryStyle()
                textSize = UiTheme.TEXT_BODY_SM
                minHeight = dp(UiTheme.BUTTON_HEIGHT)
                minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
                setOnClickListener {
                    if (gameState.supplies >= slotCost) {
                        gameState.supplies -= slotCost
                        gameState.maxWarbandSlots++
                        showFeedback("Squadron expanded")
                        refreshUI()
                        rebuildUpgradeSection()
                    } else {
                        showFeedback("Need $slotCost fuel")
                    }
                }
            }
            upgradeSection.addView(upgradeBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun showFeedback(message: String) {
        feedbackBanner.text = message
        feedbackBanner.visibility = View.VISIBLE
        feedbackBanner.alpha = 0f
        feedbackBanner.translationY = dp(8).toFloat()
        feedbackBanner.animate().alpha(1f).translationY(0f).setDuration(160L).withEndAction {
            feedbackBanner.postDelayed({
                feedbackBanner.animate().alpha(0f).translationY(dp(8).toFloat()).setDuration(140L).withEndAction {
                    feedbackBanner.visibility = View.GONE
                }.start()
            }, 1200L)
        }.start()
    }

    private fun showDismissConfirmation(squadId: String, unitName: String) {
        val rootFrame = window.decorView.findViewById<FrameLayout>(android.R.id.content)
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC0A0F1A"))
            isClickable = true
        }
        val dismiss = {
            overlay.animate().alpha(0f).setDuration(140L).withEndAction {
                rootFrame.removeView(overlay)
            }.start()
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiTheme.gradientSurface(
                topHex = UiTheme.SURFACE_ELEVATED, bottomHex = UiTheme.SURFACE,
                borderHex = UiTheme.BORDER, radius = UiTheme.RADIUS_LG
            )
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5))
            elevation = dpF(UiTheme.SHEET_ELEVATION)
        }
        panel.addView(TextView(this).apply {
            text = "Stand Down $unitName?"
            textSize = UiTheme.TEXT_CARD_TITLE
            typeface = UiTheme.TYPEFACE_HEADING
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
        })
        panel.addView(TextView(this).apply {
            text = "This flight will be permanently stood down from your squadron."
            textSize = UiTheme.TEXT_SECONDARY
            typeface = UiTheme.TYPEFACE_BODY
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            setPadding(0, dp(UiTheme.SPACE_2), 0, dp(UiTheme.SPACE_4))
        })
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(this).apply {
            text = "Cancel"
            applySecondaryStyle()
            textSize = UiTheme.TEXT_BUTTON_SM
            minHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
            minimumHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
            setOnClickListener { dismiss() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(UiTheme.SPACE_2)
        })
        btnRow.addView(Button(this).apply {
            text = "Stand Down"
            applyPrimaryStyle(UiTheme.HOSTILE)
            textSize = UiTheme.TEXT_BUTTON_SM
            minHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
            minimumHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
            setOnClickListener {
                dismiss()
                gameState.removeSquad(squadId)
                refreshUI()
            }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(btnRow)

        overlay.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ).apply { marginStart = dp(UiTheme.SPACE_5); marginEnd = dp(UiTheme.SPACE_5) })
        overlay.alpha = 0f
        rootFrame.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        overlay.animate().alpha(1f).setDuration(160L).start()
    }

    private fun refreshUI() {
        suppliesText.text = "  ${gameState.supplies} FUEL   ${gameState.renown} RDNS"
        AppCompatResources.getDrawable(this, R.drawable.ic_lucide_fuel)?.let { d ->
            DrawableCompat.setTint(d.mutate(), Color.parseColor(UiTheme.WARNING))
            d.setBounds(0, 0, dp(12), dp(12))
            suppliesText.setCompoundDrawables(d, null, null, null)
            suppliesText.compoundDrawablePadding = dp(4)
        }
        slotsText.text = "  ${gameState.warband.size}/${gameState.maxWarbandSlots} FLIGHTS"
        AppCompatResources.getDrawable(this, R.drawable.ic_lucide_users)?.let { d ->
            DrawableCompat.setTint(d.mutate(), Color.parseColor(UiTheme.TEXT_MUTED))
            d.setBounds(0, 0, dp(12), dp(12))
            slotsText.setCompoundDrawables(d, null, null, null)
            slotsText.compoundDrawablePadding = dp(4)
        }
        squadList.removeAllViews()

        for (squad in gameState.warband) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiTheme.roundedRect(UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_MD)
                setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
                elevation = dpF(UiTheme.CARD_ELEVATION)
            }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Category dot
            val catDot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(getCategoryColor(squad.unitType.category))
                }
            }
            header.addView(catDot, LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(UiTheme.SPACE_2) })

            header.addView(TextView(this).apply {
                text = "${squad.unitType.name} x${squad.count}"
                textSize = UiTheme.TEXT_BODY
                setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
                typeface = UiTheme.TYPEFACE_HEADING
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            header.addView(TextView(this).apply {
                text = squad.unitType.category.name
                textSize = UiTheme.TEXT_CHIP
                setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
                typeface = UiTheme.TYPEFACE_LABEL
                letterSpacing = 0.06f
                background = UiTheme.roundedRect(UiTheme.SURFACE_ALT, null, UiTheme.RADIUS_XS)
                setPadding(dp(UiTheme.SPACE_2), dp(2), dp(UiTheme.SPACE_2), dp(2))
            })
            card.addView(header)

            // Stats
            card.addView(TextView(this).apply {
                text = "ATK ${squad.unitType.baseAttack}  ·  DEF ${squad.unitType.baseDefense}  ·  " +
                    "SPD ${"%.1f".format(squad.unitType.baseSpeed)}  ·  RNG ${"%.1f".format(squad.unitType.range)}"
                textSize = UiTheme.TEXT_SECONDARY
                setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
                typeface = UiTheme.TYPEFACE_BODY
                setPadding(0, dp(UiTheme.SPACE_2), 0, 0)
            })

            // HP / Morale bar
            val hpColor = when {
                squad.currentHpPercent > 0.6f -> UiTheme.POSITIVE
                squad.currentHpPercent > 0.3f -> UiTheme.WARNING
                else -> UiTheme.HOSTILE
            }
            card.addView(TextView(this).apply {
                text = "HULL ${"%.0f".format(squad.currentHpPercent * 100)}%  ·  Confidence ${"%.0f".format(squad.morale)}%"
                textSize = UiTheme.TEXT_SECONDARY
                setTextColor(Color.parseColor(hpColor))
                typeface = UiTheme.TYPEFACE_LABEL
                setPadding(0, dp(UiTheme.SPACE_1), 0, 0)
            })

            // Description
            card.addView(TextView(this).apply {
                text = squad.unitType.description
                textSize = UiTheme.TEXT_MICRO + 2f
                setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
                typeface = UiTheme.TYPEFACE_BODY
                setPadding(0, dp(UiTheme.SPACE_1), 0, 0)
                maxLines = 2
            })

            // Dismiss action
            card.addView(TextView(this).apply {
                text = "Stand Down"
                textSize = UiTheme.TEXT_SECONDARY
                setTextColor(Color.parseColor(UiTheme.HOSTILE_MUTED))
                typeface = UiTheme.TYPEFACE_LABEL
                gravity = Gravity.END
                setPadding(0, dp(UiTheme.SPACE_2), 0, 0)
                setOnClickListener {
                    showDismissConfirmation(squad.id, squad.unitType.name)
                }
            })

            squadList.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(UiTheme.SPACE_3) })
        }

        // Empty slots
        val emptySlots = gameState.maxWarbandSlots - gameState.warband.size
        for (i in 0 until emptySlots) {
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dpF(UiTheme.RADIUS_MD)
                    setColor(Color.parseColor(UiTheme.SURFACE_ALT))
                    setStroke(UiTheme.STROKE_WIDTH, Color.parseColor(UiTheme.BORDER))
                }
                setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_4))
            }
            emptyCard.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(UiTheme.BORDER))
                }
            }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(UiTheme.SPACE_2) })
            emptyCard.addView(TextView(this).apply {
                text = "Open Slot"
                textSize = UiTheme.TEXT_BODY_SM
                setTextColor(Color.parseColor(UiTheme.TEXT_DISABLED))
                typeface = UiTheme.TYPEFACE_BODY
            })
            squadList.addView(emptyCard, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(UiTheme.SPACE_3) })
        }
    }

    private fun getCategoryColor(category: UnitCategory): Int {
        return when (category) {
            UnitCategory.FIGHTER -> Color.parseColor("#5A79B8")
            UnitCategory.INTERCEPTOR -> Color.parseColor("#7D92C2")
            UnitCategory.BOMBER -> Color.parseColor("#5B9B78")
            UnitCategory.RECON -> Color.parseColor(UiTheme.PRIMARY)
            UnitCategory.SUPPORT -> Color.parseColor("#5A94A8")
        }
    }

    private fun marginParams(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottom); topMargin = dp(top) }
    }
}

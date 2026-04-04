package com.warpath.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.warpath.model.UnitCategory
import com.warpath.model.UnitType

class WarbandActivity : AppCompatActivity() {

    private lateinit var squadList: LinearLayout
    private lateinit var suppliesText: TextView
    private val gameState get() = CampaignActivity.campaignManager.gameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val canRecruit = intent.getBooleanExtra("can_recruit", false)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1a1a2e"))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // Title
        val title = TextView(this).apply {
            text = "Your Warband"
            textSize = 28f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(title, marginParams(bottom = 8))

        // Supplies
        suppliesText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#33aa33"))
            gravity = Gravity.CENTER
        }
        layout.addView(suppliesText, marginParams(bottom = 24))

        // Squad slots header
        val slotsText = TextView(this).apply {
            text = "Squads (${gameState.warband.size}/${gameState.maxWarbandSlots})"
            textSize = 16f
            setTextColor(Color.parseColor("#8888aa"))
        }
        layout.addView(slotsText, marginParams(bottom = 12))

        // Squad list
        squadList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(squadList, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Recruit button
        if (canRecruit) {
            val recruitSection = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 30, 0, 0)
            }
            val recruitTitle = TextView(this).apply {
                text = "Recruit New Troops"
                textSize = 18f
                setTextColor(Color.parseColor("#cc8833"))
                typeface = Typeface.DEFAULT_BOLD
            }
            recruitSection.addView(recruitTitle, marginParams(bottom = 12))

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
                val btn = Button(this).apply {
                    text = "${unitType.name} x$count - $cost supplies"
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#2a2a4e"))
                    isAllCaps = false
                    setPadding(20, 12, 20, 12)
                    stateListAnimator = null
                    setOnClickListener {
                        if (CampaignActivity.campaignManager.recruitUnit(unitType, count, cost)) {
                            Toast.makeText(this@WarbandActivity, "Recruited ${unitType.name}!", Toast.LENGTH_SHORT).show()
                            refreshUI()
                        } else if (gameState.supplies < cost) {
                            Toast.makeText(this@WarbandActivity, "Not enough supplies!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@WarbandActivity, "Warband is full!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                recruitSection.addView(btn, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 })
            }

            layout.addView(recruitSection)
        }

        // Upgrade warband slots
        val upgradeSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 30, 0, 0)
        }
        if (gameState.maxWarbandSlots < 6) {
            val slotCost = gameState.maxWarbandSlots * 40
            val upgradeBtn = Button(this).apply {
                text = "Expand Warband (+1 slot) - $slotCost supplies"
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#6633aa"))
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 16, 20, 16)
                stateListAnimator = null
                setOnClickListener {
                    if (gameState.supplies >= slotCost) {
                        gameState.supplies -= slotCost
                        gameState.maxWarbandSlots++
                        Toast.makeText(this@WarbandActivity, "Warband expanded!", Toast.LENGTH_SHORT).show()
                        refreshUI()
                    } else {
                        Toast.makeText(this@WarbandActivity, "Not enough supplies!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            upgradeSection.addView(upgradeBtn)
        }
        layout.addView(upgradeSection)

        // Back button
        val backBtn = Button(this).apply {
            text = "Back"
            textSize = 16f
            setTextColor(Color.parseColor("#aaaacc"))
            setBackgroundColor(Color.TRANSPARENT)
            isAllCaps = false
            setOnClickListener { finish() }
        }
        layout.addView(backBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 30 })

        scroll.addView(layout)
        setContentView(scroll)
        refreshUI()
    }

    private fun refreshUI() {
        suppliesText.text = "Supplies: ${gameState.supplies} | Renown: ${gameState.renown}"
        squadList.removeAllViews()

        for (squad in gameState.warband) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#222244"))
                setPadding(24, 16, 24, 16)
            }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Color indicator
            val colorDot = View(this).apply {
                setBackgroundColor(getCategoryColor(squad.unitType.category))
            }
            header.addView(colorDot, LinearLayout.LayoutParams(16, 16).apply { marginEnd = 12 })

            val nameTv = TextView(this).apply {
                text = "${squad.unitType.name} x${squad.count}"
                textSize = 16f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
            header.addView(nameTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val catTv = TextView(this).apply {
                text = squad.unitType.category.name
                textSize = 12f
                setTextColor(Color.parseColor("#888899"))
            }
            header.addView(catTv)

            card.addView(header)

            // Stats row
            val stats = TextView(this).apply {
                text = "ATK:${squad.unitType.baseAttack} DEF:${squad.unitType.baseDefense} " +
                    "SPD:${"%.1f".format(squad.unitType.baseSpeed)} RNG:${"%.1f".format(squad.unitType.range)}"
                textSize = 12f
                setTextColor(Color.parseColor("#8888aa"))
            }
            card.addView(stats, marginParams(top = 4))

            // HP / Morale
            val hpMorale = TextView(this).apply {
                text = "HP: ${"%.0f".format(squad.currentHpPercent * 100)}% | Morale: ${"%.0f".format(squad.morale)}%"
                textSize = 12f
                setTextColor(when {
                    squad.currentHpPercent > 0.6f -> Color.parseColor("#33cc33")
                    squad.currentHpPercent > 0.3f -> Color.parseColor("#cccc33")
                    else -> Color.parseColor("#cc3333")
                })
            }
            card.addView(hpMorale, marginParams(top = 2))

            // Description
            val desc = TextView(this).apply {
                text = squad.unitType.description
                textSize = 11f
                setTextColor(Color.parseColor("#666688"))
            }
            card.addView(desc, marginParams(top = 4))

            // Dismiss button
            val dismissBtn = TextView(this).apply {
                text = "Dismiss"
                textSize = 12f
                setTextColor(Color.parseColor("#cc5555"))
                gravity = Gravity.END
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    AlertDialog.Builder(this@WarbandActivity)
                        .setTitle("Dismiss ${squad.unitType.name}?")
                        .setMessage("This squad will be permanently removed.")
                        .setPositiveButton("Dismiss") { _, _ ->
                            gameState.removeSquad(squad.id)
                            refreshUI()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            card.addView(dismissBtn)

            squadList.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 })
        }

        // Empty slots
        val emptySlots = gameState.maxWarbandSlots - gameState.warband.size
        for (i in 0 until emptySlots) {
            val emptyCard = TextView(this).apply {
                text = "[ Empty Slot ]"
                textSize = 14f
                setTextColor(Color.parseColor("#444466"))
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#181833"))
                setPadding(24, 20, 24, 20)
            }
            squadList.addView(emptyCard, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 })
        }
    }

    private fun getCategoryColor(category: UnitCategory): Int {
        return when (category) {
            UnitCategory.FRONTLINE -> Color.parseColor("#3366aa")
            UnitCategory.SKIRMISH -> Color.parseColor("#6699cc")
            UnitCategory.RANGED -> Color.parseColor("#339966")
            UnitCategory.CAVALRY -> Color.parseColor("#6633aa")
            UnitCategory.SUPPORT -> Color.parseColor("#3399aa")
        }
    }

    private fun marginParams(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = bottom; topMargin = top }
    }
}

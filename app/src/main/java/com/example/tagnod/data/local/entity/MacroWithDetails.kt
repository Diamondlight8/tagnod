package com.example.tagnod.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MacroWithDetails(
    @Embedded
    val macro: MacroEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "macroId"
    )
    val tags: List<NfcTagEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "macroId"
    )
    val actions: List<ActionEntity>
)

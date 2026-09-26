package com.example.tagnod.domain.model

import com.example.tagnod.data.local.entity.ActionEntity
import org.json.JSONObject

data class ActionModel(
    val id: Long = 0,
    val macroId: Long = 0,
    val actionOrder: Int = 0,
    val type: ActionType,
    val params: Map<String, String> = emptyMap()
) {
    fun toEntity(): ActionEntity {
        val jsonObject = JSONObject()
        params.forEach { (key, value) ->
            jsonObject.put(key, value)
        }
        return ActionEntity(
            id = id,
            macroId = macroId,
            actionOrder = actionOrder,
            actionType = type.name,
            paramsJson = jsonObject.toString()
        )
    }

    companion object {
        fun fromEntity(entity: ActionEntity): ActionModel {
            val type = ActionType.fromString(entity.actionType)
            val params = mutableMapOf<String, String>()
            try {
                if (entity.paramsJson.isNotBlank()) {
                    val jsonObject = JSONObject(entity.paramsJson)
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        params[key] = jsonObject.optString(key, "")
                    }
                }
            } catch (_: Exception) {
            }

            return ActionModel(
                id = entity.id,
                macroId = entity.macroId,
                actionOrder = entity.actionOrder,
                type = type,
                params = params
            )
        }
    }
}

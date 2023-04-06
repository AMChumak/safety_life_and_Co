package com.example.navigationandmap.ui.models


data class PointsOnRoard(
    val points: ArrayList<PointModelClass>
)

data class PointModelClass (
    val id:Int,
    val x: Int,
    val y: Int,
    val size: Int,
    val type: String
)


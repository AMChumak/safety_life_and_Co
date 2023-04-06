package com.example.navigationandmap.ui.models

data class QuadrantsOnMap(
    val quadrants: ArrayList<QuadrantModelClass>
)

data class QuadrantModelClass (
    val id: Int,
    val xA: Int,
    val yA: Int,
    val xB: Int,
    val yB: Int,
    val xC: Int,
    val yC: Int,
    val road_points_IDs: Array<Int>
)
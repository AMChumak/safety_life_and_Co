package com.example.navigationandmap.ui

data class User(
    var inDangerous: Boolean = false,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var direction: Double = 0.0,
    var userX: Double = 0.0,
    var userY: Double = 0.0,
    var userMovedX: Double = 0.0,
    var userMovedY: Double = 0.0,
    var closestPoint: Int = 0,
    var pointX: Double = 0.0,
    var pointY: Double = 0.0,
    var type: Int = -1,
    var sizeRoad: Int = 0,
    var dist: Double = 0.0,
)

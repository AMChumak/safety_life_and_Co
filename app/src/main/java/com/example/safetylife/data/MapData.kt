package com.example.safetylife.data

const val topLatitude: Double = 53.394963
const val startLongitude: Double = 83.640446
const val bottomLatitude: Double = 53.318944
const val endLongitude: Double = 83.807644
const val widthInPixels: Double = 2360.0
const val heightInPixels: Double = 1799.0
const val rangeUpdate: Double = 110.0
const val rangeSuitable: Double = 16384.0
const val quotienHorizontal: Double = widthInPixels / (endLongitude - startLongitude)
const val quotientVertical: Double = heightInPixels / (topLatitude - bottomLatitude)
const val maxDistanceBetweenPointsOnSameRoad: Double = 10.4
const val pedestrianSpeed: Double = 3.0


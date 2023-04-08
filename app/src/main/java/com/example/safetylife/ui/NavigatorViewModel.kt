package com.example.navigationandmap.ui
import android.util.Log
import androidx.lifecycle.ViewModel

import com.example.navigationandmap.ui.models.PointModelClass
import com.example.navigationandmap.ui.models.QuadrantModelClass
import com.example.navigationandmap.ui.models.QuadrantsOnMap
import com.example.safetylife.data.*
import kotlin.math.*

class NavigatorViewModel : ViewModel() {
    private val TAG = "ViewModel"

    private val _uiState = User()
    val uiState: User
        get() = _uiState

    private val mapPoints: MutableList<PointModelClass> = ArrayList()
    private val mapQuadrants: MutableList<QuadrantModelClass> = ArrayList()

    private val closestPoints: MutableList<PointModelClass> = ArrayList()

    private var lastX: Double = 0.0
    private var lastY: Double = 0.0

    private var centerUpdateZoneX: Double = 0.0
    private var centerUpdateZoneY: Double = 0.0

    private var minimalDistance: Double = 50000.0

    private var userVector: Array<Double> = arrayOf(0.0, 0.0)


    // переменные для тестирования
    private var myPoint = PointModelClass(1,1,1,1,"d")
    private var typeCon: Int = -1
    private var minDist: Double = -1.0


    fun updateCoordinates(
        currentLatitude: Double,
        currentLongitude: Double,
        currentDirection: Double
    ) {
        var isInDangerous: Boolean = false
        createUserVector(currentDirection)
        lastX = (currentLongitude - startLongitude) * quotienHorizontal
        lastY = (topLatitude - currentLatitude) * quotientVertical



        if (countDistanceToPoint(
                centerUpdateZoneX,
                centerUpdateZoneY,
                lastX,
                lastY
            ) >= rangeUpdate
        ) {
            centerUpdateZoneX = lastX
            centerUpdateZoneY = lastY
            updateCurrentPointArray()
        }

        isInDangerous = checkDanger()


        _uiState.latitude = currentLatitude
        _uiState.longitude = currentLongitude
        _uiState.inDangerous = isInDangerous
        _uiState.direction = currentDirection
        _uiState.userX = lastX
        _uiState.userY = lastY
        _uiState.userMovedX = lastX + userVector[0]
        _uiState.userMovedY = lastY + userVector[1]
        _uiState.closestPoint = myPoint.id
        _uiState.sizeRoad = myPoint.size
        _uiState.pointX = myPoint.x.toDouble()
        _uiState.pointY = myPoint.y.toDouble()
        _uiState.type = typeCon
        _uiState.dist = minDist

        // прибавляем координаты компаса
        lastX += userVector[0]
        lastY += userVector[1]

        isInDangerous = checkDanger()

        _uiState.inDangerous = isInDangerous || uiState.inDangerous
    }

    private fun checkDanger(): Boolean{

        val closestRoadPoints: ArrayList<PointModelClass> = ArrayList()

        var curPointInd: Int = findClosestPoint(lastX, lastY)
        // для тестирования
        myPoint = closestPoints[curPointInd]

        minDist = -1.0
        if (curPointInd != -1) {
            closestRoadPoints.add(closestPoints[curPointInd])
            closestPoints.removeAt(curPointInd)
            curPointInd = findClosestPoint(
                closestRoadPoints[0].x.toDouble(),
                closestRoadPoints[0].y.toDouble()
            )
            closestRoadPoints.add(closestPoints[curPointInd])
            closestPoints.removeAt(curPointInd)
            curPointInd = findClosestPoint(
                closestRoadPoints[0].x.toDouble(),
                closestRoadPoints[0].y.toDouble()
            )
            closestRoadPoints.add(closestPoints[curPointInd])
            closestPoints.removeAt(curPointInd)
            closestPoints.addAll(closestRoadPoints)

            var curDistance = countDistanceToPoint(
                closestRoadPoints[0].x.toDouble(),
                closestRoadPoints[0].y.toDouble(),
                closestRoadPoints[1].x.toDouble(),
                closestRoadPoints[1].y.toDouble()
            )
            if (curDistance < maxDistanceBetweenPointsOnSameRoad) {
                minimalDistance = countMinDistanceToSegment(
                    closestRoadPoints[0].x.toDouble(),
                    closestRoadPoints[0].y.toDouble(),
                    closestRoadPoints[1].x.toDouble(),
                    closestRoadPoints[1].y.toDouble(),
                    lastX,
                    lastY
                )
                // для тестирования
                typeCon = 0
            }
            curDistance = countDistanceToPoint(
                closestRoadPoints[0].x.toDouble(),
                closestRoadPoints[0].y.toDouble(),
                closestRoadPoints[2].x.toDouble(),
                closestRoadPoints[2].y.toDouble()
            )
            if (curDistance < maxDistanceBetweenPointsOnSameRoad) {

                if ( minimalDistance > countMinDistanceToSegment(
                        closestRoadPoints[0].x.toDouble(),
                        closestRoadPoints[0].y.toDouble(),
                        closestRoadPoints[2].x.toDouble(),
                        closestRoadPoints[2].y.toDouble(),
                        lastX,
                        lastY
                    )){
                    minimalDistance = countMinDistanceToSegment(
                        closestRoadPoints[0].x.toDouble(),
                        closestRoadPoints[0].y.toDouble(),
                        closestRoadPoints[2].x.toDouble(),
                        closestRoadPoints[2].y.toDouble(),
                        lastX,
                        lastY
                    )
                    // для тестирования
                    typeCon = 1
                }

            }
            if ( minimalDistance > countDistanceToPoint(
                    lastX,
                    lastY,
                    closestRoadPoints[0].x.toDouble(),
                    closestRoadPoints[0].y.toDouble()
                )){
                minimalDistance = countDistanceToPoint(
                    lastX,
                    lastY,
                    closestRoadPoints[0].x.toDouble(),
                    closestRoadPoints[0].y.toDouble()
                )
                // для тестирования
                typeCon = 2
            }
            minDist = minimalDistance

            if (minimalDistance < ceil(closestRoadPoints[0].size.toDouble().pow(2) / 3))
                return true
        }
        return false
    }

    fun loadMapPointsAndQuadrants(
        points: ArrayList<PointModelClass>,
        quadrants: ArrayList<QuadrantModelClass>
    ) {
        mapPoints.addAll(points)
        mapQuadrants.addAll(quadrants)
        updateCurrentPointArray()
    }

    private fun updateCurrentPointArray() {
        val templatePointIds: MutableList<Int> = arrayListOf()
        closestPoints.clear()
        for (quadrant in mapQuadrants) {
            if ((quadrant.xA - centerUpdateZoneX).pow(2) + (quadrant.yA - centerUpdateZoneY).pow(2) <= rangeSuitable ||
                (quadrant.xA - centerUpdateZoneX).pow(2) + (quadrant.yB - centerUpdateZoneY).pow(2) <= rangeSuitable ||
                (quadrant.xB - centerUpdateZoneX).pow(2) + (quadrant.yA - centerUpdateZoneY).pow(2) <= rangeSuitable ||
                (quadrant.xB - centerUpdateZoneX).pow(2) + (quadrant.yB - centerUpdateZoneY).pow(2) <= rangeSuitable
            ) {
                quadrant.road_points_IDs.forEach {
                    templatePointIds.add(it)
                }
            }
        }
        for (point in mapPoints) {
            if (point.id in templatePointIds && point !in closestPoints) {
                closestPoints.add(point)
            }
        }
        var pointInd = 0
        while (pointInd < closestPoints.size) {
            if (closestPoints[pointInd].id !in templatePointIds) {
                closestPoints.removeAt(pointInd)
            } else {
                pointInd++
            }
        }
    }


    private fun findClosestPoint(centerX: Double, centerY: Double): Int {
        var minDistancePointInd: Int = -1
        var minDistance = 1000000000.0
        var pointInd = 0
        Log.d(TAG, "count available points ${closestPoints.size}")
        while (pointInd < closestPoints.size) {
            if ((closestPoints[pointInd].x.toDouble() - centerX).pow(2) +
                (closestPoints[pointInd].y.toDouble() - centerY).pow(2) < minDistance
            ) {
                minDistance =
                    (closestPoints[pointInd].x.toDouble() - centerX).pow(2) +
                            (closestPoints[pointInd].y.toDouble() - centerY).pow(2)
                minDistancePointInd = pointInd
            }
            pointInd++
        }
        Log.d(TAG, "minDistanceInd ${minDistancePointInd}")
        return minDistancePointInd
    }

    private fun countMinDistanceToSegment(
        segmentBeginX: Double,
        segmentBeginY: Double,
        segmentEndX: Double,
        segmentEndY: Double,
        externalPointX: Double,
        externalPointY: Double
    ): Double {
        val segmentVector: Array<Double> =
            arrayOf(segmentEndX - segmentBeginX, segmentEndY - segmentBeginY)
        val normalVector: Array<Double> =
            arrayOf(segmentEndY - segmentBeginY, segmentBeginX - segmentEndX)
        val vectorToSegment: Array<Double> =
            arrayOf(segmentBeginX - externalPointX, segmentBeginY - externalPointY)
        val projectionQuotient: Double =
            (normalVector[0] * vectorToSegment[0] + normalVector[1] * vectorToSegment[1]) /
                    (normalVector[0].pow(2) + normalVector[1].pow(2))
        val projectionOfVectorToSegment: Array<Double> =
            arrayOf(projectionQuotient * normalVector[0], projectionQuotient * normalVector[1])
        if ((vectorToSegment[0] - projectionOfVectorToSegment[0]) * segmentVector[0] + (vectorToSegment[1] - projectionOfVectorToSegment[1]) * segmentVector[1] < 0) {
            return projectionOfVectorToSegment[0].pow(2) + projectionOfVectorToSegment[1].pow(2)
        } else {
            return countDistanceToPoint(
                externalPointX,
                externalPointY,
                segmentBeginX,
                segmentBeginY
            )
        }
    }

    private fun countDistanceToPoint(
        segmentBeginX: Double,
        segmentBeginY: Double,
        segmentEndX: Double,
        segmentEndY: Double
    ): Double {
        return (segmentBeginX - segmentEndX).pow(2) + (segmentBeginY - segmentEndY).pow(2)
    }

    private fun createUserVector(angle: Double) {
        if (angle <= 90.0) {
            Log.d(TAG, "cos${cos(PI/2 - angle / 180.0 * PI)}")
            userVector[0] = cos(PI/2 - angle / 180.0 * PI) * pedestrianSpeed
            userVector[1] = -sin(PI/2 - angle / 180.0 * PI) * pedestrianSpeed
        } else if (angle <= 180.0) {
            userVector[0] = cos(angle / 180.0 * PI - PI/2) * pedestrianSpeed
            userVector[1] = sin(angle / 180.0 * PI - PI/2) * pedestrianSpeed
        } else if (angle <= 270.0) {
            userVector[1] = cos(angle / 180.0 * PI - PI) * pedestrianSpeed
            userVector[0] = -sin(angle / 180.0 * PI - PI) * pedestrianSpeed
        } else {
            userVector[0] = -cos(angle / 180.0 * PI - 3 * PI/2) * pedestrianSpeed
            userVector[1] = -sin(angle / 180.0 * PI - 3 * PI/2) * pedestrianSpeed
        }
    }

}

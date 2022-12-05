package camel_case_v25_final.robot.droid;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;
import camel_case_v25_final.dijkstra.Dijkstra20;

public class Builder extends Droid {
    private MapLocation archonLocation = null;

    private boolean isLaboratoryBuilder = false;
    private MapLocation borderLocation = null;
    private int labsBuilt = 0;
    private int labStartRound = 0;

    public Builder(RobotController rc) {
        super(rc, RobotType.BUILDER, new Dijkstra20(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        lookForDangerTargets();


        if (tryRepairNearby()) {
            return;
        }

        runWatchtowerBuilder();
    }

    private void runWatchtowerBuilder() throws GameActionException {
        if (!rc.isActionReady() || rc.getTeamLeadAmount(myTeam) < 300 || rc.getRoundNum() < 100) {
            tryWander();
            return;
        }

        if (archonLocation == null) {
            archonLocation = getClosestArchon();
            if (archonLocation == null) {
                return;
            }
        }

        for (Direction direction : adjacentDirections) {
            MapLocation location = rc.adjacentLocation(direction);
            if (!rc.canSenseLocation(location)) {
                continue;
            }

            RobotInfo robot = rc.senseRobotAtLocation(location);
            if (robot != null && robot.team == myTeam && robot.mode == RobotMode.PROTOTYPE) {
                tryRepair(robot.location);
                return;
            }
        }

        Direction bestDirection = null;
        int minDistance = Integer.MAX_VALUE;

        for (Direction direction : adjacentDirections) {
            if (!rc.canBuildRobot(RobotType.WATCHTOWER, direction)) {
                continue;
            }

            MapLocation location = rc.adjacentLocation(direction);
            if (Math.abs(location.x - archonLocation.x) % 2 != 0 || Math.abs(location.y - archonLocation.y) % 2 != 0) {
                continue;
            }

            int distance = location.distanceSquaredTo(archonLocation);
            if (distance < minDistance) {
                bestDirection = direction;
                minDistance = distance;
            }
        }

        if (bestDirection != null) {
            rc.buildRobot(RobotType.WATCHTOWER, bestDirection);
        }

        tryWander();
    }

    private boolean tryRepair(MapLocation location) throws GameActionException {
        if (rc.canRepair(location)) {
            Direction bestDirection = null;
            int minRubble = rc.senseRubble(rc.getLocation());

            for (Direction direction : adjacentDirections) {
                if (!rc.canMove(direction)) {
                    continue;
                }

                MapLocation newLocation = rc.adjacentLocation(direction);
                if (newLocation.distanceSquaredTo(location) > me.actionRadiusSquared) {
                    continue;
                }

                int rubble = rc.senseRubble(newLocation);
                if (rubble < minRubble) {
                    bestDirection = direction;
                    minRubble = rubble;
                }
            }

            if (bestDirection != null) {
                tryMove(bestDirection);
            }

            rc.repair(location);
            return true;
        }

        return false;
    }

    private boolean tryRepairNearby() throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        RobotInfo repairTarget = null;
        int minDistance = Integer.MAX_VALUE;
        int minHealth = Integer.MAX_VALUE;

        for (RobotInfo robot : rc.senseNearbyRobots(me.visionRadiusSquared, myTeam)) {
            if (!robot.type.isBuilding() || robot.health == robot.type.getMaxHealth(robot.level)) {
                continue;
            }

            int distance = myLocation.distanceSquaredTo(robot.location);
            if (distance < minDistance || (distance == minDistance && robot.health < minHealth)) {
                repairTarget = robot;
                minDistance = distance;
                minHealth = robot.health;
            }
        }

        if (repairTarget != null) {
            if (minDistance <= me.actionRadiusSquared) {
                tryRepair(repairTarget.location);
            } else {
                tryMoveTo(repairTarget.location);
            }

            return true;
        }

        return false;
    }

    private void tryWanderSafe() throws GameActionException {
        if (rc.getLocation().distanceSquaredTo(borderLocation) < 20) {
            tryMoveRandom();
        } else {
            tryMoveTo(borderLocation);
        }
    }
}

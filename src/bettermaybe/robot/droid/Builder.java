package bettermaybe.robot.droid;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra20;

public class Builder extends Droid {
    private MapLocation archonLocation = null;
    private boolean isLaboratoryBuilder = false;

    private MapLocation borderLocation = null;

    public Builder(RobotController rc) {
        super(rc, RobotType.BUILDER, new Dijkstra20(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        lookForDangerTargets();

        if (!isLaboratoryBuilder) {
            isLaboratoryBuilder = !sharedArray.laboratoryBuilderAlive();
        }

        if (isLaboratoryBuilder) {
            sharedArray.markLaboratoryBuilderAlive();
        }


        if (tryRepairNearby()) {
            return;
        }

        if (borderLocation == null) {
            MapLocation myLocation = rc.getLocation();
            MapLocation center = new MapLocation(mapWidth / 2, mapHeight / 2);

            /*switch (directionBetween(center, myLocation)) {
                case NORTHEAST:
                    borderLocation = new MapLocation(mapWidth - 1, mapHeight - 1);
                    break;
                case SOUTHEAST:
                    borderLocation = new MapLocation(mapWidth - 1, 0);
                    break;
                case SOUTHWEST:
                    borderLocation = new MapLocation(0, 0);
                    break;
                case NORTHWEST:
                default:
                    borderLocation = new MapLocation(0, mapHeight - 1);
            }*/

            switch (center.directionTo(myLocation)) {
                case NORTH:
                    borderLocation = new MapLocation(mapWidth / 2, mapHeight - 1);
                    break;
                case NORTHEAST:
                    borderLocation = new MapLocation(mapWidth - 1, mapHeight - 1);
                    break;
                case EAST:
                    borderLocation = new MapLocation(mapWidth - 1, mapHeight / 2);
                    break;
                case SOUTHEAST:
                    borderLocation = new MapLocation(mapWidth - 1, 0);
                    break;
                case SOUTH:
                    borderLocation = new MapLocation(mapWidth / 2, 0);
                    break;
                case SOUTHWEST:
                    borderLocation = new MapLocation(0, 0);
                    break;
                case WEST:
                    borderLocation = new MapLocation(0, mapHeight / 2);
                    break;
                case NORTHWEST:
                default:
                    borderLocation = new MapLocation(0, mapHeight - 1);
            }
        }

        runWatchtowerBuilder();
    }

    private void runWatchtowerBuilder() throws GameActionException {
        if (archonLocation == null) {
            archonLocation = getClosestArchon();
            if (archonLocation == null) {
                return;
            }
        }
        if (!rc.isActionReady() || rc.getLocation().distanceSquaredTo(archonLocation) > 3) {
            tryMoveToArchon();
            return;
        }

        for (Direction direction : adjacentDirections) {
            MapLocation location = rc.adjacentLocation(direction);
            if (!rc.canSenseLocation(location)) {
                continue;
            }

            RobotInfo robot = rc.senseRobotAtLocation(location);
            if (robot != null && robot.team == myTeam && robot.mode == RobotMode.PROTOTYPE) {
                System.out.println("Fixing");
                tryRepair(robot.location);
                return;
            }
        }

        Direction bestDirection = null;
        int minDistance = Integer.MAX_VALUE;
        //System.out.println("I'm Here" + rc.getTeamLeadAmount(myTeam));

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
        }else{
            sharedArray.markBuilderNeedsResources();
        }

        tryMoveToArchon();
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

package bettermaybe.robot.droid;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra20;
import bettermaybe.util.BattlecodeFunction;

import java.util.Arrays;

public class Miner extends Droid {
    public Miner(RobotController rc) {
        super(rc, RobotType.MINER, new Dijkstra20(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        lookForDangerTargets();

        RobotInfo visibleTarget = getAttackTarget(me.visionRadiusSquared);
        if (visibleTarget != null
                && visibleTarget.type.canAttack()
                && rc.getHealth() != me.getMaxHealth(rc.getLevel())) {
            tryMoveToSafety();
            tryMineLeadAllDirections();
            return;
        }

        if (tryMine(rc.senseNearbyLocationsWithGold(2), this::senseGold, this::tryMineGold)) {
            tryMineLeadAllDirections();
            return;
        }

        if (tryMine(rc.senseNearbyLocationsWithLead(me.visionRadiusSquared), this::senseLead, this::tryMineLead)) {
            tryMineLeadAllDirections();
            return;
        }

        tryWander();
        tryMineLeadAllDirections();
    }

    private boolean tryMine(MapLocation[] options,
                            BattlecodeFunction<MapLocation, Integer> senseResources,
                            BattlecodeFunction<MapLocation, Boolean> tryMine) throws GameActionException {
        if (options.length == 0) {
            return false;
        }

        int[] resourcesByOption = new int[options.length];
        for (int i = 0; i < options.length; i++) {
            resourcesByOption[i] = senseResources.apply(options[i]);
        }

        MapLocation myLocation = rc.getLocation();

        int[] minersRequiredByOption = new int[options.length];
        Arrays.fill(minersRequiredByOption, 2);

        for (RobotInfo robot : rc.senseNearbyRobots(me.visionRadiusSquared, myTeam)) {
            if (robot.type != RobotType.MINER) {
                continue;
            }

            int bestIndex = -1;
            int maxResources = Integer.MIN_VALUE;
            int minDistance = Integer.MAX_VALUE;

            for (int i = 0; i < options.length; i++) {
                if (minersRequiredByOption[i] == 0) {
                    continue;
                }

                if (options[i].equals(robot.location)) {
                    bestIndex = i;
                    break;
                }

                int distance = robot.location.distanceSquaredTo(options[i]);
                if (resourcesByOption[i] > maxResources || (resourcesByOption[i] == maxResources && distance < minDistance)) {
                    bestIndex = i;
                    maxResources = resourcesByOption[i];
                    minDistance = distance;
                }
            }

            if (bestIndex > -1) {
                minersRequiredByOption[bestIndex]--;
            }
        }

        MapLocation bestOption = null;
        int maxResources = Integer.MIN_VALUE;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < options.length; i++) {
            if (minersRequiredByOption[i] == 0) {
                continue;
            }

            if (options[i].equals(myLocation)) {
                tryMine.apply(options[i]);
                return true;
            }

            int distance = myLocation.distanceSquaredTo(options[i]);
            if (resourcesByOption[i] > maxResources || (resourcesByOption[i] == maxResources && distance < minDistance)) {
                bestOption = options[i];
                maxResources = resourcesByOption[i];
                minDistance = distance;
            }
        }

        if (bestOption != null) {
            if (!myLocation.isAdjacentTo(bestOption)) {
                tryMoveTo(bestOption);
            }

            tryMine.apply(bestOption);
            return true;
        }

        return false;
    }

    private int senseGold(MapLocation location) throws GameActionException {
        return rc.senseGold(location);
    }

    private boolean tryMineGold(MapLocation location) throws GameActionException {
        if (!rc.canMineGold(location)) {
            return false;
        }

        while (rc.canMineGold(location)) {
            rc.mineGold(location);
        }

        for (Direction direction : adjacentDirections) {
            MapLocation adjacentLocation = rc.adjacentLocation(direction);
            while (rc.canMineGold(adjacentLocation)) {
                rc.mineGold(adjacentLocation);
            }
        }

        return true;
    }

    private int senseLead(MapLocation location) throws GameActionException {
        return rc.senseLead(location);
    }

    private boolean tryMineLead(MapLocation location) throws GameActionException {
        if (!rc.canMineLead(location)) {
            return false;
        }

        while (rc.canMineLead(location) && rc.senseLead(location) > 1) {
            rc.mineLead(location);
        }

        for (Direction direction : adjacentDirections) {
            MapLocation adjacentLocation = rc.adjacentLocation(direction);
            while (rc.canMineLead(adjacentLocation) && rc.senseLead(adjacentLocation) > 1) {
                rc.mineLead(adjacentLocation);
            }
        }

        return true;
    }

    private void tryMineLeadAllDirections() throws GameActionException {
        for (Direction direction : adjacentDirections) {
            MapLocation location = rc.adjacentLocation(direction);
            while (rc.canMineLead(location) && rc.senseLead(location) > 1) {
                rc.mineLead(location);
            }
        }
    }

    private int distanceToArchon() throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < 5; i++) {
            MapLocation archon = sharedArray.getMyArchonLocation(i);
            if (archon == null) {
                continue;
            }

            int distance = myLocation.distanceSquaredTo(archon);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }
}

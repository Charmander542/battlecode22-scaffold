package bettermaybe.robot.droid;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra20;

public class Soldier extends Droid {
    public Soldier(RobotController rc) {
        super(rc, RobotType.SOLDIER, new Dijkstra20(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        lookForDangerTargets();

        int myHealth = rc.getHealth();
        if (myHealth < 10 || (myHealth < 16 && distanceToArchon() < 34) || rc.getRoundNum()<200) {
            tryMoveToArchon();
        }

        RobotInfo visibleTarget = getAttackTarget(me.visionRadiusSquared);
        if (visibleTarget != null
                && visibleTarget.type.canAttack()
                && !rc.isActionReady()
                && rc.getHealth() != me.getMaxHealth(rc.getLevel())) {
            tryMoveToSafety();
            return;
        }

        //if(numSoldiers()>5){
        RobotInfo attackTarget = getAttackTarget(me.actionRadiusSquared);
        if (attackTarget != null) {
            tryAttack(attackTarget);
            return;
        }

        if (visibleTarget != null && tryMoveToAndAttack(visibleTarget.location)) {
            return;
        }

        if (tryMoveToAndAttack(getClosestDangerTarget())) {
            return;
        }

        if (tryMoveToAndAttack(getArchonTarget())) {
            return;
        }

        if (tryMoveToAndAttack(getPossibleArchonTarget())) {
            return;
        }
        //}

        tryWander();
    }

    private boolean tryMoveToAndAttack(MapLocation location) throws GameActionException {
        if (location == null) {
            return false;
        }

        tryMoveTo(location);

        if (rc.canSenseLocation(location)) {
            RobotInfo robot = rc.senseRobotAtLocation(location);
            if (robot != null) {
                tryAttack(robot);
            }
        }

        return true;
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

    private int numSoldiers() throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();


        int defenderCount = me.canAttack() ? 1 : 0;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == myTeam && robot.type==RobotType.SOLDIER){
                defenderCount++;
            }
        }
        return defenderCount;
    }
}

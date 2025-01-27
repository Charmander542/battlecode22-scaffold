package player36;

import battlecode.common.*;

public class Targeting {

    static MapLocation findNearestEmptyTile(RobotController rc, MapLocation me) throws GameActionException {
        MapLocation[] nearbyTiles = rc.getAllLocationsWithinRadiusSquared(me, 100);
        int minDistance = 10000;
        MapLocation nearestFreeTile = null;

        for (MapLocation tile : nearbyTiles) {
            if (rc.canSenseLocation(tile) && !rc.canSenseRobotAtLocation(tile) && rc.senseLead(tile) == 0 && rc.senseGold(tile) == 0) {
                if (nearestFreeTile == null || me.distanceSquaredTo(tile) < minDistance) {
                    nearestFreeTile = tile;
                    minDistance = me.distanceSquaredTo(nearestFreeTile);
                }
            }
        }

        return nearestFreeTile;
    }
    /**
     * Returns a location of an enemy archon, or null if none found.
     * 
     * @param rc
     * @return
     * @throws GameActionException
     */
    static MapLocation getOffensiveTarget(RobotController rc) throws GameActionException {
        EnemyLocation enemyLocation = Communications.getOffensiveLocation(rc);

        
        if (!enemyLocation.exists) {
            return null;
        } else {

            if (rc.canSenseLocation(enemyLocation.location)
                    && rc.canSenseRobotAtLocation(enemyLocation.location)
                    && rc.senseRobotAtLocation(enemyLocation.location).type != RobotType.ARCHON) {
                Communications.clearOffensiveLocation(rc);
                return null;
            } else {
                return enemyLocation.location;
            }
        }
    }



    /**
     * Returns nearest allied archon, or null if they aren't in need of defense.
     * 
     * @param rc
     * @return
     * @throws GameActionException
     */


    static MapLocation getDefensiveTarget(RobotController rc) throws GameActionException {
        ArchonLocation[] archonLocations = Communications.getArchonLocations(rc);
        MapLocation me = rc.getLocation();
        
        double minDistance = 1000000;
        MapLocation minLocation = null;
        for (ArchonLocation archonLocation : archonLocations) {
            if (archonLocation.exists && archonLocation.shouldDefend) {
                if (minLocation == null) {
                    minLocation = archonLocation.location;
                    minDistance = me.distanceSquaredTo(minLocation);
                } else if (me.distanceSquaredTo(archonLocation.location) < minDistance) {
                    minLocation = archonLocation.location;
                    minDistance = me.distanceSquaredTo(minLocation);
                }
            }
        }

        return minLocation;
    }


    /**
     * Returns nearest target. These are enemies in vision, in the shared array, or a provided backup location. If None of these exist, returns null. If you recieve null, you should update the backupTarget to a new random location.
     * 
     * @param rc
     * @param nearbyEnemies
     * @return
     * @throws GameActionException
     */
    static MapLocation getTargetFromGlobalAndLocalEnemyLocations(RobotController rc, RobotInfo[] nearbyEnemies, MapLocation backupTarget) throws GameActionException {
        MapLocation me = rc.getLocation();
        EnemyLocation[] globalEnemyLocations = Communications.getEnemyLocations(rc);
        MapLocation target = null;
        boolean foundGlobalTarget = false;
        boolean foundDangerousTarget = false;
        boolean foundLocalTarget = false;
        int lowestHPDangerousEnemy = 100000;
        int lowestHPBenignEnemy = 100000;
        double closestDistanceToGlobalEnemy = 100000;

        if (nearbyEnemies.length == 0) {
            for (EnemyLocation globalEnemy : globalEnemyLocations) {
                
                if (globalEnemy == null || !globalEnemy.exists) {
                    break;
                }
    
                // System.out.println(globalEnemy.location.toString());
                
                boolean clearedGlobalEnemyFromArray = false;
    
                if (rc.canSenseLocation(globalEnemy.location)) {
                    if (!rc.canSenseRobotAtLocation(globalEnemy.location) || rc.senseRobotAtLocation(globalEnemy.location).team.equals(rc.getTeam())) {
                        Communications.clearEnemyLocation(rc, globalEnemy.index);
                        clearedGlobalEnemyFromArray = true;
                    }
                }


    
                if (!clearedGlobalEnemyFromArray && me.distanceSquaredTo(globalEnemy.location) < closestDistanceToGlobalEnemy) {
                    target = globalEnemy.location;
                    closestDistanceToGlobalEnemy = me.distanceSquaredTo(target);
                }
            }
        }

        for (RobotInfo localEnemy : nearbyEnemies) {
            if (localEnemy.type.equals(RobotType.SOLDIER)
            || localEnemy.type.equals(RobotType.SAGE)
            || localEnemy.type.equals(RobotType.WATCHTOWER)) {
                if (localEnemy.health < lowestHPDangerousEnemy) {
                    target = localEnemy.location;
                    lowestHPDangerousEnemy = localEnemy.health;
                    foundDangerousTarget = true;
                    foundLocalTarget = true;
                }
            }

            if (!foundDangerousTarget) {
                if (localEnemy.health < lowestHPBenignEnemy) {
                    target = localEnemy.location;
                    lowestHPBenignEnemy = localEnemy.health;
                    foundLocalTarget = true;
                }
            }
        }


        if (target != null && foundDangerousTarget) {
            rc.setIndicatorString("reached setEnemyLoc");
        }

        if (target != null && foundLocalTarget && rc.senseRobotAtLocation(target).type == RobotType.ARCHON) {
            rc.setIndicatorString("reached setEnemyLoc");
            Communications.setOffensiveLocation(rc, target);
        } else if (target != null && foundLocalTarget) {
            Communications.setEnemyLocation(rc, target, globalEnemyLocations);
        }


        if (target == null) {
            target = backupTarget;
            if (me.distanceSquaredTo(backupTarget) <= 4) {
                target = null;
            }
        }

        return target;
    }

    static MapLocation getFallbackTarget(MapLocation me, MapLocation target) {
        int x = (target.x - me.x);
        int y = (target.y - me.y);
        return new MapLocation(me.x - x, me.y - y);
    }

}

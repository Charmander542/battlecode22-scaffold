package player36;


import battlecode.common.*;


strictfp class Archon {
    static int[] sharedArray = new int[64];
    static int radiusSquared = RobotType.ARCHON.visionRadiusSquared;
    static int buildLimit = 250;
    static boolean buildlimeReached = false;
    static int enemyleadAmount = 0;
    static int leadAmount = 0;
    static boolean noBuilder = false;
    // Bytecodes:
    // Builds a unit in target direction. If target direction is CENTER, picks dir randomly.
    static void buildUnit(RobotController rc, RobotType type, Direction dir) throws GameActionException {
        if (dir.equals(Direction.CENTER)) {
            int dirIndex = RobotPlayer.rng.nextInt(RobotPlayer.directions.length); 
            dir = RobotPlayer.directions[dirIndex];
        }
        
        Direction oldDir = dir;
        int numDirections = 8;
        for (int i = 0; i < numDirections; i++) {
            if (rc.canBuildRobot(type, dir)) {
                rc.buildRobot(type, dir);
                break;
            } else {
                dir = dir.rotateLeft();
                if (dir.equals(oldDir)) {
                    break;
                }
            }
        }
    }

    static MapLocation rotateLocation(MapLocation original, int mapWidth, int mapHeight) {
        int newX = (mapWidth - 1) - original.x;
        int newY = (mapHeight - 1) - original.y;
        int translateX = newX - original.x;
        int translateY = newY - original.y;
        MapLocation reflectedLocation = original.translate(translateX, translateY);
        return reflectedLocation;
    }

    static MapLocation reflectLocationByX(MapLocation original, int mapWidth, int mapHeight) {
        int newX = (mapWidth - 1) - original.x;
        int translateX = newX - original.x;
        MapLocation reflectedLocation = original.translate(translateX, 0);
        return reflectedLocation;
    }

    static MapLocation reflectLocationByY(MapLocation original, int mapWidth, int mapHeight) {
        int newY = (mapHeight-1) - original.y;
        int tranlateY = newY - original.y;
        return original.translate(0, tranlateY);
    }

    // index 1 will be for defense location, 0 will be for offense location.
    static void setDefendLocation(RobotController rc, MapLocation loc, int id) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        int lastEditor = (bitvector & (0x3 << 12)) >> 12;
        int xCoord = (bitvector & (0x3f << 6)) >> 6;
        int yCoord = (bitvector & (0x3f));
        if (((lastEditor == id) && (yCoord != 0 && xCoord != 0)) || ((yCoord == 0) && (xCoord == 0))) {
            lastEditor = id;
            xCoord = loc.x;
            yCoord = loc.y;
            bitvector = 0;
            bitvector += loc.y;
            bitvector += loc.x << 6;
            bitvector += lastEditor << 12;
            rc.writeSharedArray(1, bitvector);
        }
    }

    static void resetDefendLocation(RobotController rc, int id) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        int lastEditor = (bitvector & (0x3 << 12)) >> 12;
        int xCoord = (bitvector & (0x3f << 6)) >> 6;
        int yCoord = (bitvector & 0x3f);
        if ((lastEditor == id && (yCoord != 0 && xCoord != 0)) || (yCoord == 0 && xCoord == 0)) {
            rc.writeSharedArray(1, 0);
        }
    }

    static boolean executeBuildStrategy(RobotController rc, MapLocation me, int round, int leadAmount, int archonCount, int roundCutoff, ArchonLocation[] archLocs, RobotInfo[] enemies, RobotInfo[] allies) throws GameActionException {
        boolean isBuilder = false;
        MapLocation[] locsWithLead = rc.senseNearbyLocationsWithLead(-1);
        boolean overTenLead = false;
        for (MapLocation loc : locsWithLead) {
            if (rc.senseLead(loc) > 10) {
                overTenLead = true;
                break;
            }
        }


        int soldier = 20;
        if (rc.getMapHeight() <= 25 && rc.getMapWidth() <= 25) {
            noBuilder = true;
            rc.setIndicatorString("no builder");
            soldier = 80;
        }

        if (round == 1) { // Set targets and build a miner if we see lead
            MapLocation firstTarget = rotateLocation(me, rc.getMapWidth(), rc.getMapHeight());
            EnemyLocation enemyArch = Communications.getOffensiveLocation(rc);

            if (!enemyArch.exists) {
                Communications.setOffensiveLocation(rc, firstTarget);
            } else {
                if (enemyArch.location.equals(me)) {
                    firstTarget = reflectLocationByX(me, rc.getMapWidth(), rc.getMapHeight());
                    Communications.setOffensiveLocation(rc, firstTarget);
                    Communications.setEnemyLocation(rc, reflectLocationByY(me, rc.getMapWidth(), rc.getMapHeight()), Communications.getEnemyLocations(rc));
                }
            }
            if (locsWithLead.length > 0 && leadAmount >= 50) {
                buildUnit(rc, RobotType.MINER, me.directionTo(locsWithLead[0]));
                return true;
            }

        } else if (rc.getTeamGoldAmount(rc.getTeam()) >= 20) {
            buildUnit(rc, RobotType.SAGE, Direction.CENTER);
            return true;
        } else{

            // Build if enemies are near
            if (enemies.length > 0 && rc.getTeamGoldAmount(rc.getTeam()) >= 20) {
                buildUnit(rc, RobotType.SAGE, Direction.CENTER);
                return false;
            }

            if (enemies.length > 0 && leadAmount >= 70) {
                buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
                return true;
            }

            // Don't build if another archon needs to be defended.
            for (ArchonLocation archLoc : archLocs) {
                if (archLoc != null && archLoc.exists && archLoc.shouldDefend && !archLoc.location.equals(me)) {
                    return false;
                }
            }

            if (round < roundCutoff) { // determined by map size
                isBuilder = true;
                for ( RobotInfo info : rc.senseNearbyRobots(RobotType.ARCHON.visionRadiusSquared, rc.getTeam())) {
                    if (info.getType() == RobotType.BUILDER) {
                        isBuilder = false;
                        break;
                    }
                }
                if (isBuilder && enemies.length == 0 && !noBuilder) {
                    buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
                } else if (enemies.length == 0){
                    buildUnit(rc, RobotType.MINER, Direction.CENTER);
                }
                return true;
            }
//////////////////////////////////////////////////////////////////////////////////////////////
            // build builder to repair archon if health is below 350
                isBuilder = true;
                for ( RobotInfo info : rc.senseNearbyRobots(RobotType.ARCHON.visionRadiusSquared, rc.getTeam())) {
                    if (info.getType() == RobotType.BUILDER) {
                        isBuilder = false;
                        break;
                    }
                }

                if (isBuilder && !noBuilder) {
                    buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
                    return true;
                }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
            boolean canBuildMiner = true;
            int minerCount = 0;
            for (RobotInfo ally : allies) {
                if (ally.type.equals(RobotType.MINER)) {
                    minerCount++;
                }
            }
            if (minerCount > 1) {
                canBuildMiner = false;
            }

            if (!noBuilder && minerCount == 0 && overTenLead && leadAmount > 50) {
                buildUnit(rc, RobotType.MINER, Direction.CENTER);
                return true;
            }
    //////////////////////////////////////////////////////////////////////////////////////////////////////
            if (enemies.length > 0 && rc.getTeamGoldAmount(rc.getTeam()) >= 20) {
                buildUnit(rc, RobotType.SAGE, Direction.CENTER);
                return false;
            }

            int n = RobotPlayer.rng.nextInt(100);
            int m = RobotPlayer.rng.nextInt(100);

            if (leadAmount > buildLimit && (m  > 100 / archonCount || leadAmount > 300)) {
                if (n < soldier) {
                    buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
                    return true;
                } else if (canBuildMiner) {
                    buildUnit(rc, RobotType.MINER, Direction.CENTER);
                    return true;
                }
            }
        }
        return false;
        
        
        // if (round < roundCutoff) {
        //     if (randomInteger < (100 / archonCount) && leadAmount > 100) {
        //         buildUnit(rc, RobotType.MINER, Direction.CENTER);
        //     }
        //     if (leadAmount > 200) {
        //         buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
        //     }
            

        // } else {
        //     // if (leadAmount > 500) {
        //     //     buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
        //     // }
        //     int n = RobotPlayer.rng.nextInt(100);
        //     if (randomInteger < 25 || leadAmount >= 100) {
        //         if (n < 82) {
        //             buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
        //         } else if (n < 98) {
        //             buildUnit(rc, RobotType.MINER, Direction.CENTER);
        //         } else {
        //             // buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
        //         }
        //     }
        //     if (leadAmount > 200) {
        //         buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
        //     }
            
        // }
        // return false;
    }

    static boolean goingTowardMainArchon = true;
    static int transformTimer = 100;
    static int movingCounter = 0;
    static int repairCount = 0;

    static void run(RobotController rc) throws GameActionException {

        transformTimer++;
        int archonCount = rc.getArchonCount();
        ArchonLocation[] archLocs = Communications.getArchonLocations(rc);
        int roundCutoff = 10;
        if (RobotPlayer.mapHeight <= 20 && RobotPlayer.mapWidth <= 20) {
                roundCutoff = 7;
        } else if (RobotPlayer.mapHeight <= 30 && RobotPlayer.mapWidth <= 30) {
            roundCutoff = 14;
        } else if (RobotPlayer.mapHeight <= 40 && RobotPlayer.mapWidth <= 40) {
            roundCutoff = 20;
        } else {
            roundCutoff = 30;
        }

        if (rc.getTeamLeadAmount(rc.getTeam()) > buildLimit) {
            buildlimeReached = true;
        }
        if(buildlimeReached && rc.getTeamLeadAmount(rc.getTeam()) < buildLimit){
            buildLimit = 90;
        }
        if (rc.getRoundNum()%100 == 0){
            buildLimit = 250;
            buildlimeReached = false;
        }

        if (archonCount <= 2) {
            roundCutoff = 20;
        }
        
        MapLocation target = new MapLocation(1000, 1000);
        EnemyLocation[] globalEnemyLocations = Communications.getEnemyLocations(rc);
        MapLocation me = rc.getLocation();
        

        MapLocation moveTarget = target;
        int minDistance = 100000;
        int minRubble = 100000;

        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(me, 10000)) {
            if (rc.canSenseLocation(loc) && rc.senseRubble(loc) <= minRubble) {
                if (rc.senseRubble(loc) < minRubble) {
                    minDistance = me.distanceSquaredTo(loc);
                    minRubble = rc.senseRubble(loc);
                    moveTarget = loc;
                }
                else if (me.distanceSquaredTo(loc) < minDistance) {
                    moveTarget = loc;
                    minDistance = me.distanceSquaredTo(loc);
                    minRubble = rc.senseRubble(loc);
                }
            }
        }
        
        int round = rc.getRoundNum();

        if (round > 50 && !moveTarget.equals(me) && roundCutoff > 15 && rc.getTeamLeadAmount(rc.getTeam()) < 100) {
            if (rc.isTransformReady() && rc.getMode().equals(RobotMode.TURRET) && rc.canTransform()) {
                rc.transform();
            }
        }

        if (rc.getMode().equals(RobotMode.PORTABLE)) {
            if (me.equals(moveTarget)) {
                if (rc.isTransformReady() && rc.canTransform()) {
                    rc.transform();
                }
            }

            if (rc.isMovementReady()) {
                Movement.move(rc, moveTarget, target, 2, false);
            }
        }

        int rcId = rc.getID();
        int id = rcId % 2 == 0 ? (rcId - 1) / 2 : (rcId - 2) / 2;
        leadAmount = rc.getTeamLeadAmount(rc.getTeam());
        enemyleadAmount = rc.getTeamLeadAmount(rc.getTeam().opponent());
        boolean movingAndFighting = false;
        
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] alliedLocs = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());

        boolean dangerClose = false;

        for (RobotInfo enemy : enemiesInRange) {
            if (enemy.type.equals(RobotType.SOLDIER) || enemy.type.equals(RobotType.SAGE) || enemy.type.equals(RobotType.WATCHTOWER) || enemy.type.equals(RobotType.MINER)) {
                dangerClose = true;
            }
        }

        if (round % 8 == 0) {
            Communications.clearArchonLocations(rc);
            Communications.clearEnemyLocations(rc);
        }

        RobotInfo[] enemyLocs = rc.senseNearbyRobots(radiusSquared, RobotPlayer.opponent);

        if (enemyLocs.length > 0) {
            // MapLocation enemyLocation = enemyLocs[0].location;
            // setDefendLocation(rc, enemyLocation, id);

            Communications.setArchonLocation(rc, me, true, id);
            Communications.setEnemyLocation(rc, enemyLocs[0].location, globalEnemyLocations);
            buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);

        } else {
            Communications.setArchonLocation(rc, me, false, id);
        }

        executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff, archLocs, enemiesInRange, alliedLocs);

        // if (round < 30) {
        //     executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff, archLocs, enemiesInRange, alliedLocs);
        // } else if (id == 0) {


        //     // clear shared buffer
        //     // for (int i = 2; i < 64; i++) {
        //     //     rc.writeSharedArray(i, 0);
        //     // }
        //     movingAndFighting = false;
        //     executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff, archLocs, enemiesInRange, alliedLocs);;
        // } else {
        //     executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff, archLocs, enemiesInRange, alliedLocs);
        // }


        // Repair before build to save lead in long run.

        for (RobotInfo ally : alliedLocs) {
            if (!rc.isActionReady()) {
                break;
            }
            if (ally.health < ally.type.health) {
                if (rc.canRepair(ally.location)) {
                    rc.repair(ally.location);
                    repairCount++;
                }
            }
        }



        

        if (movingAndFighting) {

            EnemyLocation oTarget = Communications.getOffensiveLocation(rc);
            EnemyLocation[] targets = Communications.getEnemyLocations(rc);

            for (EnemyLocation loc : targets) {
                if (me.distanceSquaredTo(loc.location) < me.distanceSquaredTo(target)) {
                    target = loc.location;
                }
            }

            if (oTarget.exists && me.distanceSquaredTo(oTarget.location) < me.distanceSquaredTo(target)) {
                target = oTarget.location;
            }
            

            int distanceToTarget = (int)Math.sqrt(me.distanceSquaredTo(target));
            int visionDistance = (int)Math.sqrt(rc.getType().visionRadiusSquared);

            if (rc.getMode() == RobotMode.TURRET) {
                if (rc.canTransform()) {
                    if (distanceToTarget > visionDistance + 10 && !dangerClose && transformTimer > 100) {
                        rc.transform();
                        transformTimer = 0;
                    }
                } else {
                    if (rc.isActionReady()) {
                        executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff, archLocs, enemiesInRange, alliedLocs);
                    }
                }
            }

            if (rc.getMode() == RobotMode.PORTABLE) {
                movingCounter++;

                if (goingTowardMainArchon) {
                    target = archLocs[0].location;

                    if (me.distanceSquaredTo(target) <= 25) {
                        goingTowardMainArchon = false;
                    }
                }

                if (round >= 100 && (distanceToTarget < visionDistance || dangerClose || movingCounter > 30) && !goingTowardMainArchon) {
                    if (rc.canTransform() && rc.senseRubble(me) < 10) {
                        rc.transform();
                        movingCounter = 0;
                    } else { // flee
                        RobotPlayer.move2(rc, rc.adjacentLocation(me.directionTo(target).opposite()), 1);
                    }
                } else {
                    RobotPlayer.move2(rc, target, 4);
                }
            }
        } else {
            if (rc.getMode() == RobotMode.PORTABLE) {
                if (rc.canTransform()) {
                    rc.transform();
                }
            }
        }

        // Differential strategies based on round number

        Team tm = rc.getTeam();
        int leadAmt = rc.getTeamLeadAmount(tm);




        for (EnemyLocation loc : globalEnemyLocations) {
            if (loc != null && loc.exists) {
                rc.setIndicatorDot(loc.location, 0, 1000, 0);
            }
        }

        // if (rc.readSharedArray(2) == 0) {
        // int start = Clock.getBytecodeNum();
        // EnemyLocation target = Comms.getOffensiveLocation(rc);
        // int end = Clock.getBytecodeNum();
        // rc.setIndicatorString("set loc: " + (end - start));

        rc.setIndicatorLine(me, target, 100, 100, 100);
        rc.setIndicatorString("Repairs: " + repairCount);

        double x;

        int startTime = Clock.getBytecodeNum();
        
        x = 10.0 / 4.0;

        int endTime = Clock.getBytecodeNum();
        rc.setIndicatorString("Time: " + (endTime - startTime));

    }
}

package bettermaybe.robot.building;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra34;
import bettermaybe.util.Utils;

public class Archon extends Building {
    private Direction[] spawnDirections;

    private boolean isFirstRun = true;
    private boolean isSoldierSpawner = false;
    private MapLocation borderLocation = null;

    private MapLocation optimalLocation = null;
    private boolean hasFoundInitialOptimalLocation = false;

    private AnomalyScheduleEntry[] anomalySchedule;

    private int minersSpawned = 0;
    private int maxLeadingMiners;
    private int firstMovingRound = 15;
    private int solfierRound = 50;

    private RobotType[] spawnOrder = {
            RobotType.SOLDIER,
            RobotType.SOLDIER,
            RobotType.SOLDIER,
            RobotType.BUILDER,
            //RobotType.SAGE
            RobotType.MINER
            
    };

    private boolean isMoving = false;

    private int spawnOrderIndex = 0;

    public Archon(RobotController rc) {
        super(rc, RobotType.ARCHON, new Dijkstra34(rc));

        setSpawnDirections();
    }

    @Override
    public void run() throws GameActionException {
        super.run();

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

        int turnIndex = sharedArray.getArchonTurnIndex();
        int archonCount = rc.getArchonCount();

        if (isFirstRun) {
            sharedArray.setMyArchonLocation(sharedArray.archonIdToIndex(rc.getID()), rc.getLocation());

            int knownArchonLocations = 0;
            for (int i = 0; i < 5; i++) {
                if (sharedArray.getMyArchonLocation(i) != null) {
                    knownArchonLocations++;
                }
            }
            if(knownArchonLocations==1){
                isSoldierSpawner = true;
            }

            if (knownArchonLocations == rc.getArchonCount()) {
                setPossibleEnemyArchonLocations();
            }

            maxLeadingMiners = Math.max(rc.senseNearbyLocationsWithLead(2).length, 5);

            isFirstRun = false;
        }

        if (turnIndex == 0) {
            sharedArray.expireDangerTargets();
        }

        lookForDangerTargets();


        boolean checkForInitialOptimalLocation = false;
        if (!hasFoundInitialOptimalLocation) {
            int currentRound = rc.getRoundNum();
            if (currentRound == 150 || (currentRound == 10 && rc.senseRubble(rc.getLocation()) >= 30)) {
                checkForInitialOptimalLocation = true;
                hasFoundInitialOptimalLocation = true;
            }
        }

        if (checkForInitialOptimalLocation || vortexHappened()) {
            findOptimalLocation();
        }

        /*if(rc.getRoundNum()>firstMovingRound && rc.getRoundNum()<100 && isMoving){
            if(rc.isTransformReady() && rc.getMode().equals(RobotMode.TURRET) && rc.canTransform()){
                rc.transform();
            }

            if (rc.getMode().equals(RobotMode.PORTABLE)) {
                tryWanderSafe();
            }
        }else{
            if(rc.isTransformReady() && rc.getMode().equals(RobotMode.PORTABLE) && rc.canTransform()){
                rc.transform();
            } 
        }*/

        if (getAttackTarget(me.visionRadiusSquared) != null && rc.getTeamLeadAmount(myTeam) > 30) {
            tryBuildRobot(RobotType.SOLDIER);
            tryRepair();
            return;
        }

        if (rc.getRoundNum() > 1 && !Utils.chance(((double) turnIndex + 1) / (double) archonCount)) {
            tryRepair();
            return;
        }

        boolean hasDangerTargets = false;
        for (int i = 0; i < Utils.MAX_DANGER_TARGETS; i++) {
            if (sharedArray.getDangerTarget(i) != null) {
                hasDangerTargets = true;
                break;
            }
        }

        if ((sharedArray.laboratoryBuilderAlive() || rc.getRoundNum()<50) && !hasDangerTargets && (minersSpawned < maxLeadingMiners || (sharedArray.builderNeedsResources() || minersSpawned < 30))) {
            if (tryBuildRobot(RobotType.MINER)) {
                minersSpawned++;
            }

            //tryBuildRobot(RobotType.SAGE);
            tryRepair();
            return;
        }

        if((rc.getHealth()<700 && !hasDangerTargets) || (sharedArray.laboratoryBuilderAlive() && rc.getRoundNum()>150)){
            tryBuildRobot(RobotType.BUILDER);
        }

        if (rc.getTeamLeadAmount(myTeam) < 600 || hasDangerTargets) {
             while ((spawnOrder[spawnOrderIndex] == RobotType.BUILDER && ((turnIndex != 0 || sharedArray.laboratoryBuilderAlive()) || rc.getRoundNum()<150)) || (spawnOrder[spawnOrderIndex] == RobotType.SOLDIER && !isSoldierSpawner)) {
                spawnOrderIndex = (spawnOrderIndex + 1) % spawnOrder.length;
            }
        }
        
        System.out.print(spawnOrder[spawnOrderIndex]);
        if((spawnOrder[spawnOrderIndex] == RobotType.MINER && minersSpawned<30) || spawnOrder[spawnOrderIndex] != RobotType.MINER){
        if (tryBuildRobot(spawnOrder[spawnOrderIndex])) {
            spawnOrderIndex = (spawnOrderIndex + 1) % spawnOrder.length;
        }
    }
        if(isSoldierSpawner || rc.getTeamLeadAmount(myTeam)>350 || rc.getArchonCount()==1){
        tryBuildRobot(RobotType.SAGE);
        tryBuildRobot(RobotType.SOLDIER);
        }
        tryRepair();
    }

    private void setSpawnDirections() {
        spawnDirections = new Direction[8];

        MapLocation center = new MapLocation(mapWidth / 2, mapHeight / 2);
        spawnDirections[0] = rc.getLocation().directionTo(center);

        spawnDirections[1] = spawnDirections[0].rotateLeft();
        spawnDirections[2] = spawnDirections[0].rotateRight();

        spawnDirections[3] = spawnDirections[1].rotateLeft();
        spawnDirections[4] = spawnDirections[2].rotateRight();

        spawnDirections[5] = spawnDirections[3].rotateLeft();
        spawnDirections[6] = spawnDirections[4].rotateRight();

        spawnDirections[7] = spawnDirections[0].opposite();
    }

    private void setPossibleEnemyArchonLocations() throws GameActionException {
        MapLocation[] locations = new MapLocation[15];

        for (int i = 0; i < 5; i++) {
            MapLocation myArchon = sharedArray.getMyArchonLocation(i);
            if (myArchon == null) {
                continue;
            }

            locations[3 * i] = new MapLocation(mapWidth - myArchon.x - 1, myArchon.y);
            locations[3 * i + 1] = new MapLocation(myArchon.x, mapHeight - myArchon.y - 1);
            locations[3 * i + 2] = new MapLocation(mapWidth - myArchon.x - 1, mapHeight - myArchon.y - 1);
        }

        for (int i = 0; i < 5; i++) {
            MapLocation myArchon = sharedArray.getMyArchonLocation(i);
            if (myArchon == null) {
                continue;
            }

            for (int j = 0; j < 15; j++) {
                if (myArchon.equals(locations[j])) {
                    locations[j] = null;
                    break;
                }
            }
        }

        for (int i = 0; i < 15; i++) {
            if (locations[i] != null) {
                sharedArray.setPossibleEnemyArchonLocation(i, locations[i]);
            }
        }
    }

    private boolean tryBuildRobot(RobotType type) throws GameActionException {
        Direction bestDirection = null;
        int minRubble = Integer.MAX_VALUE;

        for (Direction direction : spawnDirections) {
            if (rc.canBuildRobot(type, direction)) {
                int rubble = rc.senseRubble(rc.adjacentLocation(direction));
                if (rubble < minRubble) {
                    minRubble = rubble;
                    bestDirection = direction;
                }
            }
        }

        if (bestDirection != null) {
            if(type == RobotType.MINER && (minersSpawned>=10 && rc.getRoundNum()<300)) return false;
            if(type == RobotType.MINER) minersSpawned++;
            rc.buildRobot(type, bestDirection);
            return true;
        }

        return false;
    }

    private void tryRepair() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }

        RobotInfo repairTarget = getRepairTarget(me.actionRadiusSquared);
        if (repairTarget != null
                && repairTarget.health < repairTarget.type.getMaxHealth(repairTarget.level)
                && rc.canRepair(repairTarget.location)) {
            rc.repair(repairTarget.location);
        }
    }

    private void findOptimalLocation() throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        int minArchonRubble = rc.senseRubble(myLocation);
        double minSpawnRubble = getSpawnRubble(myLocation);
        int minDistance = 0;

        for (MapLocation location : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 20)) {
            int archonRubble = rc.senseRubble(location);
            if (archonRubble > minArchonRubble) {
                continue;
            }

            if (myLocation.equals(location) || rc.senseRobotAtLocation(location) != null) {
                continue;
            }

            double spawnRubble = getSpawnRubble(location);
            int distance = myLocation.distanceSquaredTo(location);

            if (archonRubble < minArchonRubble
                    || spawnRubble < minSpawnRubble
                    || (spawnRubble == minSpawnRubble && distance < minDistance)) {
                optimalLocation = location;
                minArchonRubble = archonRubble;
                minSpawnRubble = spawnRubble;
                minDistance = distance;
            }
        }
    }

    private boolean vortexHappened() {
        if (anomalySchedule == null) {
            anomalySchedule = rc.getAnomalySchedule();
        }

        int round = rc.getRoundNum() - 1;
        for (AnomalyScheduleEntry entry : anomalySchedule) {
            if (entry.roundNumber == round && entry.anomalyType == AnomalyType.VORTEX) {
                return true;
            }
        }

        return false;
    }

    private double getSpawnRubble(MapLocation location) throws GameActionException {
        double score = 0.0;

        MapLocation center = new MapLocation(mapWidth / 2, mapHeight / 2);
        Direction optimalDirection = location.directionTo(center);

        MapLocation optimalLocation = location.add(optimalDirection);
        if (rc.onTheMap(optimalLocation)) {
            score += rc.senseRubble(optimalLocation);
        }

        Direction left = optimalDirection;
        Direction right = optimalDirection;

        for (int i = 0; i < 3; i++) {
            left = left.rotateLeft();
            right = right.rotateRight();

            MapLocation locationLeft = location.add(left);
            if (rc.onTheMap(locationLeft)) {
                score += (double) (i + 2) * rc.senseRubble(locationLeft);
            }

            MapLocation locationRight = location.add(right);
            if (rc.onTheMap(locationRight)) {
                score += (double) (i + 2) * rc.senseRubble(locationRight);
            }
        }


        MapLocation oppositeLocation = location.add(optimalDirection.opposite());
        if (rc.onTheMap(oppositeLocation)) {
            score += 5.0 * rc.senseRubble(oppositeLocation);
        }

        return score;
    }

    private void tryWanderSafe() throws GameActionException {
        if (rc.getLocation().distanceSquaredTo(borderLocation) < 20) {
            tryMoveRandom();
        } else {
            tryMoveTo(borderLocation);
        }
    }
}

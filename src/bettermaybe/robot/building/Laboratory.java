package bettermaybe.robot.building;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra53;

public class Laboratory extends Building {
    public Laboratory(RobotController rc) {
        super(rc, RobotType.LABORATORY, new Dijkstra53(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (!sharedArray.builderNeedsResources() && rc.canTransmute() && rc.getTeamLeadAmount(rc.getTeam())>600) {
            rc.transmute();
        }
    }
    
}

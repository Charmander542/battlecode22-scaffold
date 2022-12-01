package maybebetter.robot.building;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import maybebetter.*;

public class Laboratory extends Building {
    public Laboratory(RobotController rc) {
        super(rc, RobotType.LABORATORY, new Dijkstra(rc));
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (!sharedArray.builderNeedsResources() && rc.canTransmute()) {
            rc.transmute();
        }
    }
}
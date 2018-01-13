// import the API.
// See xxx for the javadocs.
import bc.*;

import java.util.PriorityQueue;

public class Player {
    static Direction[] dirs  = Direction.values();

    //map analysis global variables
    //feel free to use them
    static GameController gc;
    static PlanetMap map;
    static short[][] passabilityMat;
    static long[][] karboniteMat;   //only on Earth
    static MapLocation baseLocation;    //only on Earth
    static PriorityQueue<Tuple<MapLocation, Integer>> baseFactoryQueue; //only on Earth

    public static void main(String[] args) {
        try {
            //connect to the manager, starting the game
            gc = new GameController();

            //map analysis
            map = gc.startingMap(gc.planet());
            passabilityMat = MapAnalysis.passabilityMat(map);
            if (map.getPlanet() == Planet.Earth) {
                karboniteMat = MapAnalysis.karboniteMat(map);

                Convolver c4 = new Convolver(4);
                baseLocation = MapAnalysis.baseLocation(MapAnalysis.opennnesMat(passabilityMat, c4), map.getPlanet(), gc.team(), map.getInitial_units());
                System.out.println("Base location: " + baseLocation.getX() + ", " + baseLocation.getY());

                baseFactoryQueue = MapAnalysis.baseFactoryQueue(baseLocation, MapAnalysis.smallBase, passabilityMat, karboniteMat);
            }

            //queue research
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Worker);
            gc.queueResearch(UnitType.Rocket);
            gc.queueResearch(UnitType.Ranger);
        }
        catch(Exception e){
            System.out.println("Exception during setup");
            e.printStackTrace();
        }
        while (true) {
            try{
                //game cycle
                Econ.turn(gc);

                gc.nextTurn();
            }
            catch(Exception e){
                System.out.println("Exception during game");
                e.printStackTrace();
            }
        }
    }
}
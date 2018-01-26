import MapTools.Karbonite;
import MapTools.Passable;
import MapTools.UnionFind;
import bc.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;

public class Player {
    static Direction[] dirs  = Direction.values();

    //global variables
    static GameController gc;
    static PlanetMap map, mapEarth, mapMars;
    static Queue<Unit> worker, knight, ranger, healer, mage, factory, rocket;
	static long time1, time2;
	static long tWorker, tCombot, tRocket, tHealer;
	static DecimalFormat f;

    public static void main(String[] args) {
        try {
            //connect to the manager, starting the game
            gc = new GameController();
	        time1 = System.nanoTime();
            map = gc.startingMap(gc.planet());
            mapEarth = gc.startingMap(Planet.Earth);
            mapMars = gc.startingMap(Planet.Mars);

            MapAnalysis.setup();
            Econ.setup();

            //queue research
            gc.queueResearch(UnitType.Ranger); //25
            gc.queueResearch(UnitType.Ranger); //125
            gc.queueResearch(UnitType.Worker); //150
            gc.queueResearch(UnitType.Healer); //175
            gc.queueResearch(UnitType.Rocket); //225
            gc.queueResearch(UnitType.Ranger); //425
            gc.queueResearch(UnitType.Healer); //525
            gc.queueResearch(UnitType.Worker); //600
            gc.queueResearch(UnitType.Worker); //675
            gc.queueResearch(UnitType.Healer); //750
            gc.queueResearch(UnitType.Worker); //825
            
            ComBot.init(gc);
            /*
	        time2 = System.nanoTime();
	        f = new DecimalFormat("##.##");
	        System.out.println("â•“â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
            System.out.println("â•‘ TIME REPORT:");
            System.out.println("â•‘ Round: " + gc.round());
            System.out.println("â•‘ Setup: " + f.format((time2-time1)/1000000.0) + "ms");
	        //System.out.println("â•šâ•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�");
	         * */
        }
        catch(Exception e){
            System.out.println("Exception during setup");
            e.printStackTrace();
        }
        while (true) {
            try {
	            //game cycle
		        System.gc();
	            worker = new LinkedList<>();
	            knight = new LinkedList<>();
	            ranger = new LinkedList<>();
	            healer = new LinkedList<>();
	            mage = new LinkedList<>();
	            factory = new LinkedList<>();
	            rocket = new LinkedList<>();

	            VecUnit units = gc.myUnits();
	            //System.out.println("Units "+ units.size());
	            for (int i = 0; i < units.size(); i++) {
		            Unit temp = units.get(i);
		            if (temp.unitType() == UnitType.Worker) {
			            worker.add(temp);
			            //System.out.println("added worker");
		            } else if (temp.unitType() == UnitType.Knight)
			            knight.add(temp);
		            else if (temp.unitType() == UnitType.Ranger)
			            ranger.add(temp);
		            else if (temp.unitType() == UnitType.Healer)
			            healer.add(temp);
		            else if (temp.unitType() == UnitType.Mage)
			            mage.add(temp);
		            else if (temp.unitType() == UnitType.Factory)
			            factory.add(temp);
		            else
			            rocket.add(temp);
	            }
	            //I'll uncomment when I actually get this part to work :)
	            //MapAnalysis.turn();
	            time1 = System.nanoTime();
	            if (Player.gc.planet() == Planet.Earth) {
		            Econ.turn(gc);
	            } else
		            MarsWorker.turn(gc);
	            time2 = System.nanoTime();
	            tWorker += (time2-time1);
	            time1 = time2;
	            Rocket.turn(gc);
	            time2 = System.nanoTime();
	            tRocket += (time2-time1);
	            time1 = time2;
	            ComBot.turn();
	            time2 = System.nanoTime();
	            tCombot += (time2-time1);
	            time1 = time2;
	            Healer.run(gc);
	            time2 = System.nanoTime();
	            tHealer += (time2-time1);

	            if(gc.round() % 250 == 0) {
		            System.out.println("â•“â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
		            System.out.println("â•‘ TIME REPORT:");
		            System.out.println("â•‘ Round: " + gc.round());
		            System.out.println("â•‘ Combot: " + f.format(tCombot/1000000.0) + "ms");
		            System.out.println("â•‘ Econ+wMars: " + f.format(tWorker/1000000.0) + "ms");
		            System.out.println("â•‘ Rocket: " + f.format(tRocket/1000000.0) + "ms");
		            System.out.println("â•‘ Healer: " + f.format(tHealer/1000000.0) + "ms");
		            System.out.println("â•‘ Remaining: " + gc.getTimeLeftMs() + "ms");
		            System.out.println("â•šâ•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�â•�");
	            }

	            gc.nextTurn();
            }
            catch(Exception e){
                System.out.println("Exception during game");
                e.printStackTrace();
            }
        }
    }
}
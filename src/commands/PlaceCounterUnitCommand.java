package commands;

import domain.*;
import enums.CounterUnitType;
import enums.MagicSpellType;
import enums.ObstacleType;
import enums.RegionType;
import gamemanager.GameManager;
import networking.GameState;
import gamescreen.GameScreen;

import java.util.List;
import java.util.logging.Logger;

/**
 * This command supports all types of counters. 
 *
 */
public class PlaceCounterUnitCommand implements GameCommand {
	 	private final String start;
	    private final String destination;
	    private final RegionType regionType;
	    private final CounterUnitType aCounterUnitType;
	    private final boolean isSecret;
	    private final String senderName;

	    public PlaceCounterUnitCommand(Road road, CounterUnit counter) {
	        GameMap map = GameMap.getInstance();
	        start = map.getRoadSource(road).getName();
	        destination = map.getRoadTarget(road).getName();
	        regionType = road.getRegionType();
	        aCounterUnitType = counter.getType();
	        isSecret = counter.isSecret();
	        senderName = GameManager.getInstance().getThisPlayer().getName();
	    }

	    @Override
	    public void execute() {
			Logger.getGlobal().info("Executing PlaceTransportationCounterCommand, placing " + aCounterUnitType);
			GameMap map = GameMap.getInstance();

			Town startTown = map.getTown(start);
			Town destinationTown = map.getTown(destination);
			Road road = map.getRoadBetween(startTown, destinationTown, regionType);
			CounterUnit counter = CounterUnit.getNew(aCounterUnitType);
			assert counter instanceof TransportationCounter || counter instanceof MagicSpell
					|| counter instanceof Obstacle || counter instanceof GoldPiece;

			//Call different methods in road for different types of CounterUnit
			if (counter instanceof TransportationCounter) {
				road.setTransportationCounter((TransportationCounter) counter);
			} else if (counter instanceof Obstacle) {
				Obstacle obstacle;
				obstacle = (Obstacle) Obstacle.getNew(counter.getType());
				road.placeObstacle(obstacle);
			} else if (counter instanceof GoldPiece) {
				road.placeGoldPiece((GoldPiece) counter);
			} else if (counter instanceof MagicSpell && counter.getType() == MagicSpellType.DOUBLE) {
				Logger.getGlobal().info("Calling placeDouble inside PlaceTransportationCounterCommand");
				road.placeDouble((MagicSpell) counter);
			}

	        // remove the counter from the sending player's hand if counter is not an obstacle
	        if (!(counter instanceof Obstacle && counter.getType() == ObstacleType.TREE)) {
	        	List<CounterUnit> senderHand = GameState.instance().getPlayerByName(senderName).getHand().getCounters();
		        int toRemoveIdx = -1;
		        for (int i = 0; i < senderHand.size(); i++) {
		            if (senderHand.get(i).getType() == aCounterUnitType
		                    && senderHand.get(i).isSecret() == isSecret) {
		                toRemoveIdx = i;
		            }
		        }
		        assert toRemoveIdx >= 0; // The counter should be in the sending player's hand
				senderHand.get(toRemoveIdx).setOwned(false);
		        senderHand.remove(toRemoveIdx);
		        GameScreen.getInstance().updateAll();
	        }
	        
	    }
}
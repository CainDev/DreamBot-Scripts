import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.mouse.algorithm.MouseAlgorithm;
import org.dreambot.api.input.mouse.destination.AbstractMouseDestination;
import org.dreambot.api.input.event.impl.mouse.MouseButton;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.awt.Point;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.Canvas;

/** 
 * A sophisticated mouse movement algorithm that simulates human-like cursor patterns.
 * This implementation uses real mouse movement data with recorded timing information
 * to create natural, direction-aware cursor paths with authentic human timing patterns.
 */
public class SmartMouseMultiDir implements MouseAlgorithm {
	Canvas canvas = Client.getCanvas();
	int screenWidth = canvas.getWidth();
	int screenHeight = canvas.getHeight();
	
    private static final int[] DISTANCE_THRESHOLDS = {12, 18, 26, 39, 58, 87, 130, 190, 260, 360, 500};



    /**
     * Expected structure in mousedata.json:
     *
     * {
     *   "12": {
     *     "N":  [ [ [dxOffsets...],[dyOffsets...],[timingMs...] ], ... ],
     *     "NE": [ ... ],
     *     "E":  [ ... ],
     *     "SE": [ ... ],
     *     "S":  [ ... ],
     *     "SW": [ ... ],
     *     "W":  [ ... ],
     *     "NW": [ ... ]
     *   },
     *   "18": { ... },
     *   ...
     * }
     * 
     * Each path contains 3 arrays:
     * - X coordinate offsets
     * - Y coordinate offsets  
     * - Timing delays in milliseconds
     */
    private Map<String, Object> mouseData;

    private final Random random = new Random();
    private boolean lastActionWasRightClick = false;
    private boolean debugLogging = false;

    public SmartMouseMultiDir() {
        //Logger.log("Smart Mouse Loaded!");
        loadMouseData();
    }
    
    /**
     * Enable or disable debug logging messages
     * @param enabled true to enable debug logs, false to disable
     */
    public void setDebugLogging(boolean enabled) {
        this.debugLogging = enabled;
    }
    
    /**
     * Log a debug message if debug logging is enabled
     * @param message the message to log
     */
    private void debugLog(String message) {
        if (debugLogging) {
            Logger.log("[SmartMouse] " + message);
        }
    }

    @Override
    public boolean handleClick(MouseButton mouseButton) {
        boolean result = Mouse.getDefaultMouseAlgorithm().handleClick(mouseButton);

        if (mouseButton == MouseButton.RIGHT_CLICK) {
            lastActionWasRightClick = true;
            sleep(randomDouble(99, 243));
        } else {
            lastActionWasRightClick = false;
        }

        return result;
    }

    @Override
    public boolean handleMovement(AbstractMouseDestination destination) {
        if (!ScriptManager.getScriptManager().isRunning()) {
            debugLog("Script stopped; not moving mouse.");
            return false;
        }

        Point target = destination.getSuitablePoint();
        Point current = Mouse.getPosition();

        if (current.equals(target)) {
            debugLog("Current mouse position is already at target. No movement needed.");
            return true;
        }
		
		if (!isWithinCanvas(current) && !isWithinCanvas(target)) {
			debugLog("Mouse position and target is outside the client canvas. Skipping path-based movement and hopping.");
			Point outside_exit = Mouse.getPointOutsideScreen();
			Mouse.hop(outside_exit);
            Mouse.setPosition(target.x, target.y);
			return true;
		}

        double distance = distance(current, target);
        debugLog("handleMovement => Current: " + current + ", Target: " + target + ", Distance: " + distance);

        // Compute angle in degrees from current -> target
        double dx = target.getX() - current.getX();
        double dy = target.getY() - current.getY();
        double angleDeg = Math.toDegrees(Math.atan2(dy, dx)); // range: -180..180
        String orientation = angleTo8Direction(angleDeg);
        debugLog("Orientation determined: " + orientation);

        // Generate path from JSON offsets and timing data
        PathData pathData = generatePath(current, target, distance, orientation);
        List<Point> path = pathData.path;
        List<Double> timings = pathData.timings;
        debugLog("Generated path with " + path.size() + " points using recorded timing data.");

        // Execute movement using recorded human timing patterns
        for (int i = 0; i < path.size(); i++) {
            Point stepPoint = path.get(i);
            
            // Convert timing from milliseconds to seconds
            double stepDurationSeconds = (i < timings.size()) ? timings.get(i) / 1000.0 : 0.008;
            
            // Add small variance (±5%) to prevent perfect repeatability
            double variance = randomDouble(-0.05, 0.05) * stepDurationSeconds;
            stepDurationSeconds = Math.max(0.001, stepDurationSeconds + variance);

            // Move to step point and sleep for the recorded duration
            moveSmoothlyWithRecordedTiming(Mouse.getPosition(), stepPoint, stepDurationSeconds);
        }

        double finalDistance = distance(Mouse.getPosition(), target);
        debugLog("Final distance to target after movement: " + finalDistance);

        if (lastActionWasRightClick) {
            sleep(randomDouble(99, 243));
        }

        return finalDistance < 2;
    }
	
	private boolean isWithinCanvas(Point point) {
		return point.getX() >= 0 && point.getY() >= 0 &&
			   point.getX() < screenWidth && point.getY() < screenHeight;
	}


    /**
     * Convert angle (in degrees) to one of the 8 compass directions: N, NE, E, SE, S, SW, W, NW.
     */
    private String angleTo8Direction(double angleDeg) {
        // Normalize to [0..360)
        double a = (angleDeg + 360) % 360;

        // E = [337.5..360) + [0..22.5)
        // NE = [22.5..67.5)
        // N = [67.5..112.5)
        // NW = [112.5..157.5)
        // W = [157.5..202.5)
        // SW = [202.5..247.5)
        // S = [247.5..292.5)
        // SE = [292.5..337.5)
        if ( (a >= 337.5 && a < 360) || (a >= 0 && a < 22.5) ) {
            return "E";
        } else if (a >= 22.5 && a < 67.5) {
            return "NE";
        } else if (a >= 67.5 && a < 112.5) {
            return "N";
        } else if (a >= 112.5 && a < 157.5) {
            return "NW";
        } else if (a >= 157.5 && a < 202.5) {
            return "W";
        } else if (a >= 202.5 && a < 247.5) {
            return "SW";
        } else if (a >= 247.5 && a < 292.5) {
            return "S";
        } else {
            return "SE";
        }
    }


    
    /**
     * Move from 'start' to 'end' using recorded timing data from human movements.
     * Performs a direct hop to the target point and sleeps for the recorded duration.
     * This preserves the authentic timing patterns captured from real human movements.
     */
    private void moveSmoothlyWithRecordedTiming(Point start, Point end, double totalMovementSeconds) {
        // Convert seconds back to milliseconds for sleep()
        double sleepMs = totalMovementSeconds * 1000.0;
        
        // Move directly to the target point
        Mouse.hop(end);
        
        // Sleep for the recorded human timing duration
        sleep(sleepMs);
    }
    

    /**
     * PathData holds both position and timing information for a movement path.
     * Contains the sequence of points to visit and the corresponding timing delays.
     */
    private static class PathData {
        public final List<Point> path;
        public final List<Double> timings; // in milliseconds
        
        public PathData(List<Point> path, List<Double> timings) {
            this.path = path;
            this.timings = timings;
        }
    }

    /**
     * Build a path of points from 'start' to 'target' using offsets and timing data from mousedata.
     * Expects JSON format with 3 arrays: [x_offsets, y_offsets, time_deltas].
     * Returns PathData containing both the coordinate path and corresponding timing information.
     */
    private PathData generatePath(Point start, Point target, double distance, String orientation) {
        List<Point> path = new ArrayList<>();
        List<Double> timings = new ArrayList<>();
        List<List<Double>> offsets = getPathOffsets(distance, orientation);

        if (offsets == null || offsets.size() < 3) {
            // No valid path data found => direct single hop
            debugLog("No valid path data found for distance/orientation. Moving directly.");
            path.add(target);
            timings.add(50.0); // Default 50ms for direct movement
            return new PathData(path, timings);
        }

        List<Double> xOffsets = offsets.get(0);
        List<Double> yOffsets = offsets.get(1);
        List<Double> timeDeltas = offsets.get(2);

        debugLog("Using recorded timing data with " + timeDeltas.size() + " timing entries");
        debugLog("Using offsets -> xSize=" + xOffsets.size() + ", ySize=" + yOffsets.size());

        double dx = target.getX() - start.getX();
        double dy = target.getY() - start.getY();

        double totalOffsetX = xOffsets.stream().mapToDouble(Double::doubleValue).sum();
        double totalOffsetY = yOffsets.stream().mapToDouble(Double::doubleValue).sum();
        debugLog("Total offset from mouse => X: " + totalOffsetX + ", Y: " + totalOffsetY);

        double adjustedDx = dx - totalOffsetX;
        double adjustedDy = dy - totalOffsetY;

        double sx = start.getX();
        double sy = start.getY();

        for (int i = 0; i < xOffsets.size(); i++) {
            double t = (i + 1.0) / xOffsets.size();

            double offsetX = 0.0;
            double offsetY = 0.0;
            for (int j = 0; j <= i; j++) {
                offsetX += xOffsets.get(j);
                offsetY += yOffsets.get(j);
            }

            double newX = sx + adjustedDx * t + offsetX;
            double newY = sy + adjustedDy * t + offsetY;
            path.add(new Point((int) newX, (int) newY));
            
            // Add timing data from recorded values
            if (i < timeDeltas.size()) {
                timings.add(timeDeltas.get(i));
            } else {
                timings.add(8.0); // Fallback if timing array is shorter
            }
        }

        // Ensure final target is included
        path.add(target);
        if (!timeDeltas.isEmpty()) {
            timings.add(timeDeltas.get(Math.min(timeDeltas.size() - 1, timings.size())));
        } else {
            timings.add(8.0);
        }
        
        return new PathData(path, timings);
    }

    /**
     * For a given distance and direction, pick a random path data from the JSON.
     * Returns [x_offsets, y_offsets, time_deltas] arrays.
     */
    @SuppressWarnings("unchecked")
    private List<List<Double>> getPathOffsets(double distance, String direction) {
        String category = getDistanceCategory(distance);
        debugLog("getPathOffsets => distance=" + distance + ", category=" + category + ", direction=" + direction);

        Map<String, Object> subMap = (Map<String, Object>) mouseData.get(category);
        if (subMap == null) {
            debugLog("No data for distance category: " + category);
            return null;
        }

        // subMap has keys: "N","NE","E","SE","S","SW","W","NW"
        List<List<List<Double>>> directionPaths = (List<List<List<Double>>>) subMap.get(direction);
        if (directionPaths == null || directionPaths.isEmpty()) {
            debugLog("No paths found in JSON for category=" + category + " / direction=" + direction);
            return null;
        }

        // Randomly select one path
        int index = random.nextInt(directionPaths.size());
        List<List<Double>> selectedPath = directionPaths.get(index);

        // Validate that we have all 3 required arrays
        if (selectedPath.size() >= 3) {
            debugLog("Selected path index " + index + " with xOffsets=" + selectedPath.get(0).size() + 
                    ", yOffsets=" + selectedPath.get(1).size() + ", timingData=" + selectedPath.get(2).size());
        } else {
            debugLog("Warning: Selected path index " + index + " missing timing data (only " + selectedPath.size() + " arrays)");
        }
        
        return selectedPath;
    }



    private String getDistanceCategory(double distance) {
        for (int threshold : DISTANCE_THRESHOLDS) {
            if (distance <= threshold) {
                return String.valueOf(threshold);
            }
        }
        // If it's bigger than the largest threshold
        return String.valueOf(DISTANCE_THRESHOLDS[DISTANCE_THRESHOLDS.length - 1]);
    }

    private void sleep(double millis) {
        try {
            long ms = Math.round(millis);
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Load mouse data from resource: mousedata.json
     * Expected structure with timing data:
     * {
     *   "12": {
     *     "N":  [ [ [dx...],[dy...],[timing_ms...] ], ... ],
     *     "NE": [ ... ],
     *     ...
     *   },
     *   "18": { ... },
     *   ...
     * }
     */
    private void loadMouseData() {
        Gson gson = new Gson();
        try (InputStream is = getClass().getResourceAsStream("/mousedata.json")) {
            if (is == null) {
                debugLog("Error: mousedata.json not found in resources.");
            } else {
                Reader reader = new InputStreamReader(is);
                mouseData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                debugLog("Mouse data loaded successfully with keys: " + mouseData.keySet());
            }
        } catch (Exception e) {
            debugLog("Error loading mouse data: " + e.getMessage());
        }
    }

    private double distance(Point p1, Point p2) {
        return p1.distance(p2);
    }


}

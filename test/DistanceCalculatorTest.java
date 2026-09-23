import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for DistanceCalculator. Uses no API key, so nothing touches the
 * internet.
 *
 * @author Jaidev Gogineni
 */
public class DistanceCalculatorTest {
    private static final double VT_LAT = 37.2296;
    private static final double VT_LNG = -80.4139;

    private DistanceCalculator calc;
    private Restaurant nearby;
    private Restaurant noLocation;

    @BeforeEach
    public void setUp() {
        calc = new DistanceCalculator(null);
        nearby = new Restaurant("Chipotle", new ArrayList<>(), 1, 37.2310, -80.4250, "");
        noLocation = new Restaurant("Mystery Diner", new ArrayList<>());
    }

    @Test
    public void noApiKeyMeansStraightLineMode() {
        assertFalse(calc.isUsingGoogleMaps());
    }

    // ------------------------------------------------------------ straightLineDistance

    @Test
    public void newYorkToLosAngelesIsAbout2445Miles() {
        double miles = DistanceCalculator.straightLineDistance(40.7128, -74.0060, 34.0522, -118.2437);
        assertEquals(2445, miles, 5);
    }

    @Test
    public void straightLineRejectsBadLatitude() {
        assertThrows(IllegalArgumentException.class,
                () -> DistanceCalculator.straightLineDistance(200, 0, 0, 0));
    }

    // ------------------------------------------------------------ getDistance

    @Test
    public void samePointIsZeroMiles() {
        assertEquals(0, calc.getDistance(VT_LAT, VT_LNG, VT_LAT, VT_LNG));
    }

    @Test
    public void getDistanceRejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> calc.getDistance(0, Double.NaN, 0, 0));
    }

    @Test
    public void getDistanceToRestaurantStoresIt() {
        double miles = calc.getDistance(VT_LAT, VT_LNG, nearby);
        assertEquals(0.62, miles, 0.05);
        assertEquals(miles, nearby.getDistance());
    }

    @Test
    public void getDistanceRejectsRestaurantWithNoLocation() {
        assertThrows(IllegalArgumentException.class, () -> calc.getDistance(VT_LAT, VT_LNG, noLocation));
        assertThrows(IllegalArgumentException.class, () -> calc.getDistance(VT_LAT, VT_LNG, null));
    }

    // ------------------------------------------------------------ updateDistances

    @Test
    public void updateDistancesSkipsRestaurantsWithNoLocation() {
        calc.updateDistances(VT_LAT, VT_LNG, new ArrayList<>(Arrays.asList(nearby, noLocation, null)));
        assertFalse(Double.isNaN(nearby.getDistance()));
        assertTrue(Double.isNaN(noLocation.getDistance()));
    }

    @Test
    public void updateDistancesRejectsNullList() {
        assertThrows(IllegalArgumentException.class, () -> calc.updateDistances(VT_LAT, VT_LNG, null));
    }

    // ------------------------------------------------------------ parseCoordinates

    @Test
    public void parsesDegreesWithDirections() {
        assertArrayEquals(new double[] {VT_LAT, VT_LNG},
                DistanceCalculator.parseCoordinates("37.2296\u00B0 N, 80.4139\u00B0 W"));
    }

    @Test
    public void parsesPlainNumbers() {
        assertArrayEquals(new double[] {VT_LAT, VT_LNG},
                DistanceCalculator.parseCoordinates("37.2296, -80.4139"));
    }

    @Test
    public void unreadableLocationsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DistanceCalculator.parseCoordinates(""));
        assertThrows(IllegalArgumentException.class, () -> DistanceCalculator.parseCoordinates("Blacksburg"));
        assertThrows(IllegalArgumentException.class, () -> DistanceCalculator.parseCoordinates(null));
    }

    // ------------------------------------------------------------ travel mode

    @Test
    public void travelModeCanBeChanged() {
        calc.setTravelMode(DistanceCalculator.TravelMode.WALK);
        assertEquals(DistanceCalculator.TravelMode.WALK, calc.getTravelMode());
    }

    @Test
    public void nullTravelModeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calc.setTravelMode(null));
    }
}

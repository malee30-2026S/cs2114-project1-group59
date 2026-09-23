import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Finds the computer's live location with the Windows location service
 * (the same one the Weather and Maps apps use).
 *
 * Only call this after the user has said yes to sharing their location.
 * It can take a few seconds, so call it off the Swing thread.
 *
 * @author Jaidev Gogineni
 */
public final class LocationFinder {
    private static final int TIMEOUT_SECONDS = 20;
    // Asks Windows for a location fix and prints "latitude,longitude"
    private static final String SCRIPT = String.join("\n",
        "$ProgressPreference = 'SilentlyContinue'",
        "Add-Type -AssemblyName System.Device",
        "$watcher = New-Object System.Device.Location.GeoCoordinateWatcher",
        "[void]$watcher.TryStart($false, [TimeSpan]::FromSeconds(10))",
        "$deadline = (Get-Date).AddSeconds(12)",
        "while ($watcher.Position.Location.IsUnknown -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 250 }",
        "$here = $watcher.Position.Location",
        "$watcher.Stop()",
        "if ($here.IsUnknown) { 'UNKNOWN' } else {",
        "  $c = [Globalization.CultureInfo]::InvariantCulture",
        "  $here.Latitude.ToString($c) + ',' + $here.Longitude.ToString($c)",
        "}");

    private LocationFinder() {
    }

    /**
     * @return true if this computer has a location service we can ask
     */
    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }

    /**
     * Asks the computer where it is.
     *
     * @return {latitude, longitude}, or null if the location isn't available
     *         (location turned off in Windows settings, no Wi-Fi, not
     *         Windows, or it took too long)
     */
    public static double[] findCurrentLocation() {
        if (!isSupported()) {
            return null;
        }
        String encoded = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
        try {
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-EncodedCommand", encoded)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String[] lines = output.split("\\R");
            for (int i = lines.length - 1; i >= 0; i--) {
                try {
                    return DistanceCalculator.parseCoordinates(lines[i]);
                }
                catch (IllegalArgumentException e) {
                    // Not the coordinates line; keep looking
                }
            }
            return null;
        }
        catch (IOException e) {
            return null;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Moderator.
 */
public class ModeratorTest {
    private Moderator moderator;

    @BeforeEach
    public void setUp() {
        moderator = new Moderator();
    }

    // ------------------------------------------------------------ validateInput

    @Test
    public void normalInputIsValid() {
        assertTrue(moderator.validateInput("Mexican"));
        assertTrue(moderator.validateInput("O'Brien-Smith"));
    }

    @Test
    public void blankNullOrSymbolOnlyInputIsInvalid() {
        assertFalse(moderator.validateInput(null));
        assertFalse(moderator.validateInput("   "));
        assertFalse(moderator.validateInput("#*@^"));
    }

    @Test
    public void hugeInputIsInvalid() {
        assertFalse(moderator.validateInput("a".repeat(501)));
    }

    // ------------------------------------------------------------ validateEmail

    @Test
    public void normalEmailIsValid() {
        assertTrue(moderator.validateEmail("jaidevg@vt.edu"));
    }

    @Test
    public void malformedEmailIsInvalid() {
        assertFalse(moderator.validateEmail("jaidevg"));
        assertFalse(moderator.validateEmail("jaidev@vt"));
        assertFalse(moderator.validateEmail("two words@vt.edu"));
        assertFalse(moderator.validateEmail(""));
    }

    // ------------------------------------------------------------ moderateReviewText

    @Test
    public void cleanReviewPasses() {
        assertTrue(moderator.moderateReviewText("Great tacos, friendly staff!"));
    }

    @Test
    public void reviewWithBannedWordOrBlankFails() {
        assertFalse(moderator.moderateReviewText("this place is BADWORD1"));
        assertFalse(moderator.moderateReviewText(""));
    }

    // ------------------------------------------------------------ moderateReviewPhoto

    @Test
    public void imagePhotoPasses() {
        assertTrue(moderator.moderateReviewPhoto("burrito.JPG"));
    }

    @Test
    public void nonImagePhotoFails() {
        assertFalse(moderator.moderateReviewPhoto("virus.exe"));
        assertFalse(moderator.moderateReviewPhoto(null));
    }
}

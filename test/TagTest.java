import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for Tag.
 *
 * @author Jaidev Gogineni
 */
public class TagTest {
    @Test
    public void getNameReturnsTheName() {
        assertEquals("Mexican", new Tag("Mexican").getName());
    }

    @Test
    public void nameIsTrimmed() {
        assertEquals("Mexican", new Tag("  Mexican ").getName());
    }

    @Test
    public void tagsMatchIgnoringCase() {
        assertEquals(new Tag("Mexican"), new Tag("mexican"));
        assertEquals(new Tag("Mexican").hashCode(), new Tag("MEXICAN").hashCode());
        assertNotEquals(new Tag("Mexican"), new Tag("Italian"));
    }

    @Test
    public void nullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tag(null));
    }

    @Test
    public void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tag("   "));
    }
}

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates input and outputs
 */
public class Moderator
{
    private static Set<String> BANNED_WORDS =
        new HashSet<>(Arrays.asList("badword1", "badword2", "badword3"));

    private static Set<String> ALLOWED_PHOTO_EXTENSIONS =
        new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png"));

    /**
     * @param input
     *            the input Checks that a string is valid
     * @return boolean
     */
    public boolean validateInput(String input)
    {
        if (input == null)
        {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }
        return true;
    }


    /**
     * Checks review text validity
     * 
     * @param reviewText
     *            the input review text
     * @return boolean
     */
    public boolean moderateReviewText(String reviewText)
    {
        if (!validateInput(reviewText))
        {
            return false;
        }
        String lower = reviewText.toLowerCase();
        for (String bannedWord : BANNED_WORDS)
        {
            if (lower.contains(bannedWord))
            {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks a photo submitted with a review. For now this only validates the
     * file extension. Maybe this will change in the future.
     * 
     * @param photoFileName
     *            the file name of the photo
     * @return boolean
     */
    public boolean moderateReviewPhoto(String photoFileName)
    {
        if (photoFileName == null)
        {
            return false;
        }
        
        String lower = photoFileName.toLowerCase();
        for (String ext : ALLOWED_PHOTO_EXTENSIONS)
        {
            if (lower.endsWith(ext))
            {
                return true;
            }
        }
        return false;
    }
}

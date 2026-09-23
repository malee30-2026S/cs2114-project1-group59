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

    /** Longest input accepted, so huge pastes can't overflow anything. */
    private static final int MAX_INPUT_LENGTH = 500;

    /**
     * Checks that a string is valid: not blank, not too long, and has at
     * least one letter or number (so "#*@^" is rejected).
     *
     * @param input
     *            the input to check
     * @return boolean
     */
    public boolean validateInput(String input)
    {
        if (input == null)
        {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_INPUT_LENGTH)
        {
            return false;
        }
        for (char c : trimmed.toCharArray())
        {
            if (Character.isLetterOrDigit(c))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks that an email looks like name@domain.tld
     *
     * @param email
     *            the email to check
     * @return boolean
     */
    public boolean validateEmail(String email)
    {
        if (!validateInput(email))
        {
            return false;
        }
        return email.trim().matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}");
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

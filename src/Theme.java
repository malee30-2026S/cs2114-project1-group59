import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Virginia Tech colors (Chicago Maroon and Burnt Orange), fonts, and the
 * styled buttons, cards, and fields every screen uses.
 */
public final class Theme {
    public static final Color MAROON = new Color(134, 31, 65);
    public static final Color MAROON_DARK = new Color(99, 15, 44);
    public static final Color ORANGE = new Color(229, 117, 31);
    public static final Color ORANGE_DARK = new Color(200, 95, 16);
    public static final Color BLACK = new Color(30, 30, 30);
    public static final Color GRAY_DARK = new Color(74, 74, 74);
    public static final Color GRAY = new Color(117, 117, 117);
    public static final Color GRAY_LIGHT = new Color(217, 217, 217);
    public static final Color BACKGROUND = new Color(242, 242, 242);
    public static final Color WHITE = Color.WHITE;
    public static final Color MAROON_TINT = new Color(245, 232, 237);
    public static final Color ORANGE_TINT = new Color(253, 239, 227);
    public static final Color ERROR = new Color(176, 0, 32);
    public static final Color SUCCESS = new Color(46, 125, 50);
    public static final Color SUCCESS_TINT = new Color(232, 245, 233);

    /** How a button looks. */
    public enum ButtonStyle {
        /** Orange, for the main action on a screen. */
        PRIMARY,
        /** White with a maroon outline. */
        SECONDARY,
        /** Just text, for small or risky actions. */
        GHOST,
        /** See-through white, for buttons on the maroon header bar. */
        HEADER
    }

    private static final String FONT_FAMILY = pickFontFamily();

    private Theme() {
    }

    /**
     * Switches to the system look (so text fields and dropdowns look native)
     * and sets the default font. Call once before building any windows.
     */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            // Keep Swing's default look
        }
        Font body = font(Font.PLAIN, 14);
        for (String key : new String[] {"Label.font", "TextField.font", "TextArea.font", "ComboBox.font",
            "CheckBox.font", "Button.font", "List.font", "ToolTip.font", "OptionPane.messageFont",
            "OptionPane.buttonFont"}) {
            UIManager.put(key, body);
        }
    }

    /**
     * @return the app font in the given style (Font.PLAIN, Font.BOLD, ...)
     *         and size
     */
    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    private static String pickFontFamily() {
        if (GraphicsEnvironment.isHeadless()) {
            return Font.SANS_SERIF;
        }
        List<String> installed = Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : new String[] {"Segoe UI", "Helvetica Neue", "Arial"}) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }

    // ---------------------------------------------------------------- Text

    /**
     * @return a label with the app font
     */
    public static JLabel text(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    /**
     * @return a small rounded label, like a tag or status badge
     */
    public static JLabel chip(String text, Color fill, Color textColor) {
        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setFont(font(Font.BOLD, 12));
        chip.setForeground(textColor);
        chip.setBorder(new EmptyBorder(3, 10, 3, 10));
        return chip;
    }

    /**
     * @return a maroon section title with an orange underline
     */
    public static JComponent sectionHeader(String title) {
        JPanel holder = clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        JLabel label = text(title, Font.BOLD, 19, MAROON);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, ORANGE), new EmptyBorder(0, 0, 4, 0)));
        holder.add(label);
        holder.setBorder(new EmptyBorder(14, 2, 10, 0));
        return fullWidth(holder);
    }

    // ---------------------------------------------------------------- Layout

    /**
     * The maroon bar across the top of every screen.
     *
     * @param actions buttons shown on the right
     */
    public static JPanel headerBar(String title, String subtitle, JComponent... actions) {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(MAROON);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 4, 0, ORANGE), new EmptyBorder(14, 24, 14, 24)));

        JPanel titles = clear(new JPanel());
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(text(title, Font.BOLD, 24, WHITE));
        if (subtitle != null && !subtitle.isBlank()) {
            titles.add(text(subtitle, Font.PLAIN, 14, new Color(255, 222, 200)));
        }
        bar.add(titles, BorderLayout.WEST);

        JPanel buttons = clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        for (JComponent action : actions) {
            buttons.add(action);
        }
        JPanel centered = clear(new JPanel(new GridBagLayout()));
        centered.add(buttons);
        bar.add(centered, BorderLayout.EAST);
        return bar;
    }

    /**
     * @return a white rounded card with padding
     */
    public static RoundedPanel card(LayoutManager layout) {
        RoundedPanel card = new RoundedPanel(layout, WHITE, GRAY_LIGHT, 18);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        return card;
    }

    /**
     * @return an orange-tinted message box, for confirmations
     */
    public static JComponent banner(String message) {
        RoundedPanel banner = new RoundedPanel(new BorderLayout(), ORANGE_TINT, ORANGE, 14);
        banner.setBorder(new EmptyBorder(10, 16, 10, 16));
        banner.add(text(message, Font.BOLD, 14, ORANGE_DARK), BorderLayout.CENTER);
        return fullWidth(banner);
    }

    /**
     * @return a vertical list that always matches the width of the scroll
     *         pane it's in, so long text never makes it scroll sideways
     */
    public static JPanel verticalList() {
        ScrollablePanel list = new ScrollablePanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BACKGROUND);
        return list;
    }

    /**
     * @return a borderless scroll pane on the gray background
     */
    public static JScrollPane scroll(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /**
     * Makes a panel see-through so the card or bar behind it shows.
     */
    public static <T extends JComponent> T clear(T component) {
        component.setOpaque(false);
        return component;
    }

    /**
     * Left-aligns a component and lets it stretch sideways but not
     * vertically inside a vertical list.
     */
    public static <T extends JComponent> T fullWidth(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    // ---------------------------------------------------------------- Inputs

    /**
     * @return a flat, rounded button in the given style
     */
    public static JButton button(String text, ButtonStyle style) {
        return new FlatButton(text, style);
    }

    /**
     * Pads a text field and gives it an orange outline while typing.
     */
    public static <T extends JTextField> T styleField(T field) {
        field.setFont(font(Font.PLAIN, 14));
        field.setForeground(BLACK);
        Border normal = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_LIGHT, 1, true), new EmptyBorder(7, 10, 7, 10));
        Border focused = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ORANGE, 2, true), new EmptyBorder(6, 9, 6, 9));
        field.setBorder(normal);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(normal);
            }
        });
        return field;
    }

    /**
     * Gives a dropdown the app font and a white background.
     */
    public static <T extends JComboBox<?>> T styleCombo(T combo) {
        combo.setFont(font(Font.PLAIN, 14));
        combo.setBackground(WHITE);
        combo.setForeground(BLACK);
        Dimension size = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(size.width, Math.max(size.height, 34)));
        return combo;
    }

    // ---------------------------------------------------------------- Components

    /** A panel with rounded corners. */
    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color outline;
        private final int arc;

        /**
         * @param outline the border color, or null for none
         */
        public RoundedPanel(LayoutManager layout, Color fill, Color outline, int arc) {
            super(layout);
            this.fill = fill;
            this.outline = outline;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
        }
    }

    /** Five stars: clickable for rating, or read-only for showing a rating. */
    public static class StarRating extends JComponent {
        private static final int GAP = 6;
        private final boolean editable;
        private final int starSize;
        private int stars;
        private int hoverStars;
        private Runnable onChange;

        /**
         * @param stars    starting rating, 0 to 5
         * @param editable true if clicking changes the rating
         * @param starSize width of each star in pixels
         */
        public StarRating(int stars, boolean editable, int starSize) {
            this.stars = Math.max(0, Math.min(5, stars));
            this.editable = editable;
            this.starSize = starSize;
            setOpaque(false);
            Dimension size = new Dimension(starSize * 5 + GAP * 4, starSize);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            if (editable) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                MouseAdapter mouse = new MouseAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        hoverStars = starAt(e.getX());
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hoverStars = 0;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        setStars(starAt(e.getX()));
                    }
                };
                addMouseListener(mouse);
                addMouseMotionListener(mouse);
            }
        }

        /**
         * @return the rating, 0 (none yet) to 5
         */
        public int getStars() {
            return stars;
        }

        /**
         * @param stars 1 to 5, or 0 for none
         */
        public void setStars(int stars) {
            this.stars = Math.max(0, Math.min(5, stars));
            repaint();
            if (onChange != null) {
                onChange.run();
            }
        }

        /**
         * @param onChange runs whenever the user picks a rating
         */
        public void setOnChange(Runnable onChange) {
            this.onChange = onChange;
        }

        private int starAt(int x) {
            return Math.max(1, Math.min(5, x / (starSize + GAP) + 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int lit = editable && hoverStars > 0 ? hoverStars : stars;
            for (int i = 0; i < 5; i++) {
                g2.setColor(i < lit ? ORANGE : GRAY_LIGHT);
                g2.fill(star(i * (starSize + GAP), 0, starSize));
            }
            g2.dispose();
        }

        private static Shape star(double x, double y, double size) {
            Path2D.Double path = new Path2D.Double();
            double centerX = x + size / 2;
            double centerY = y + size / 2 + size * 0.04;
            double outer = size / 2;
            double inner = outer * 0.45;
            for (int point = 0; point < 10; point++) {
                double radius = point % 2 == 0 ? outer : inner;
                double angle = Math.toRadians(-90 + point * 36);
                double px = centerX + radius * Math.cos(angle);
                double py = centerY + radius * Math.sin(angle);
                if (point == 0) {
                    path.moveTo(px, py);
                }
                else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        }
    }

    /** A bar that grows right in orange for liked tags, left in gray for disliked. */
    public static class WeightBar extends JComponent {
        private final double weight;
        private final double maxWeight;

        /**
         * @param weight    this tag's weight
         * @param maxWeight the largest absolute weight being shown
         */
        public WeightBar(double weight, double maxWeight) {
            this.weight = weight;
            this.maxWeight = Math.max(maxWeight, 0.0001);
            setOpaque(false);
            setPreferredSize(new Dimension(220, 14));
            setMaximumSize(new Dimension(220, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int middle = getWidth() / 2;
            g2.setColor(BACKGROUND);
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 8, 8);
            int length = (int)Math.round(Math.min(1, Math.abs(weight) / maxWeight) * (middle - 2));
            g2.setColor(weight >= 0 ? ORANGE : GRAY);
            Rectangle bar = weight >= 0
                ? new Rectangle(middle, 2, length, getHeight() - 4)
                : new Rectangle(middle - length, 2, length, getHeight() - 4);
            g2.fillRoundRect(bar.x, bar.y, bar.width, bar.height, 8, 8);
            g2.setColor(GRAY_LIGHT);
            g2.drawLine(middle, 0, middle, getHeight());
            g2.dispose();
        }
    }

    /** A flat button that paints its own rounded background. */
    private static class FlatButton extends JButton {
        private final ButtonStyle style;

        FlatButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setFont(font(Font.BOLD, 14));
            setForeground(style == ButtonStyle.PRIMARY || style == ButtonStyle.HEADER ? WHITE
                : style == ButtonStyle.SECONDARY ? MAROON : GRAY_DARK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 18, 8, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            ButtonModel model = getModel();
            boolean hot = isEnabled() && (model.isRollover() || model.isPressed());
            Color fill = null;
            Color outline = null;
            switch (style) {
                case PRIMARY:
                    fill = !isEnabled() ? GRAY_LIGHT : hot ? ORANGE_DARK : ORANGE;
                    break;
                case SECONDARY:
                    fill = hot ? MAROON_TINT : WHITE;
                    outline = MAROON;
                    break;
                case HEADER:
                    fill = new Color(255, 255, 255, hot ? 70 : 35);
                    outline = new Color(255, 255, 255, 130);
                    break;
                default:
                    fill = hot ? new Color(0, 0, 0, 22) : null;
                    break;
            }
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A panel that stretches to the scroll pane's width instead of scrolling sideways. */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }
}

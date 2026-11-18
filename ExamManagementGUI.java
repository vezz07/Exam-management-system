import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * EXAM MANAGEMENT SYSTEM - GUI VERSION (Java Swing, no external libraries, no database)
 *
 * Data is stored in the "data" folder (same files as the console version), so:
 *  - first run creates the PRELOADED data automatically,
 *  - everything you add / edit / delete in the screens is saved immediately,
 *  - you may also edit the .txt files by hand (while the program is closed).
 *
 * Default admin login  : admin / admin123
 * Default student login: S101 / pass101   (S102 / pass102, S103 / pass103)
 *
 * Run:  javac ExamManagementGUI.java
 *       java ExamManagementGUI
 */
public class ExamManagementGUI {

    // =====================================================================
    //                         DATA LAYER (FILES)
    // =====================================================================
    static final Path DATA_DIR = Paths.get("data");
    static final Path STUDENTS_FILE = DATA_DIR.resolve("students.txt");
    static final Path EXAMS_FILE = DATA_DIR.resolve("exams.txt");
    static final Path QUESTIONS_FILE = DATA_DIR.resolve("questions.txt");
    static final Path RESULTS_FILE = DATA_DIR.resolve("results.txt");
    static final Path ADMIN_FILE = DATA_DIR.resolve("admin.txt");
    static final String SEP = "|";
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static class Student {
        String id, name, password, course;

        Student(String id, String name, String password, String course) {
            this.id = id;
            this.name = name;
            this.password = password;
            this.course = course;
        }

        String toLine() {
            return id + SEP + name + SEP + password + SEP + course;
        }
    }

    static class Exam {
        String id, title, subject;
        int durationMin, passPercent;

        Exam(String id, String title, String subject, int durationMin, int passPercent) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.durationMin = durationMin;
            this.passPercent = passPercent;
        }

        String toLine() {
            return id + SEP + title + SEP + subject + SEP + durationMin + SEP + passPercent;
        }
    }

    static class Question {
        String id, examId, text;
        String[] options;
        int correct; // 1..4
        int marks;

        Question(String id, String examId, String text, String[] options, int correct, int marks) {
            this.id = id;
            this.examId = examId;
            this.text = text;
            this.options = options;
            this.correct = correct;
            this.marks = marks;
        }

        String toLine() {
            return id + SEP + examId + SEP + text + SEP + options[0] + SEP + options[1] + SEP + options[2] + SEP
                    + options[3] + SEP + correct + SEP + marks;
        }
    }

    static class Result {
        String studentId, examId, dateTime;
        int score, total;
        boolean passed;

        Result(String studentId, String examId, int score, int total, String dateTime, boolean passed) {
            this.studentId = studentId;
            this.examId = examId;
            this.score = score;
            this.total = total;
            this.dateTime = dateTime;
            this.passed = passed;
        }

        double percent() {
            return total == 0 ? 0 : (score * 100.0) / total;
        }

        String toLine() {
            return studentId + SEP + examId + SEP + score + SEP + total + SEP + dateTime + SEP + passed;
        }
    }

    static List<Student> students = new ArrayList<>();
    static List<Exam> exams = new ArrayList<>();
    static List<Question> questions = new ArrayList<>();
    static List<Result> results = new ArrayList<>();
    static String adminUser = "admin";
    static String adminPass = "admin123";

    static void createDefaultFilesIfMissing() throws IOException {
        Files.createDirectories(DATA_DIR);
        if (!Files.exists(ADMIN_FILE)) {
            List<String> l = new ArrayList<>();
            l.add("# username|password");
            l.add("admin|admin123");
            writeLines(ADMIN_FILE, l);
        }
        if (!Files.exists(STUDENTS_FILE)) {
            List<String> l = new ArrayList<>();
            l.add("# studentId|name|password|course");
            l.add("S101|Arun Kumar|pass101|B.Sc Computer Science");
            l.add("S102|Divya R|pass102|B.Sc Computer Science");
            l.add("S103|Karthik S|pass103|BCA");
            writeLines(STUDENTS_FILE, l);
        }
        if (!Files.exists(EXAMS_FILE)) {
            List<String> l = new ArrayList<>();
            l.add("# examId|title|subject|durationMinutes|passPercent");
            l.add("E1|Java Basics Test|Programming|10|40");
            l.add("E2|Database Fundamentals|DBMS|10|40");
            l.add("E3|General Aptitude|Aptitude|8|50");
            writeLines(EXAMS_FILE, l);
        }
        if (!Files.exists(QUESTIONS_FILE)) {
            List<String> l = new ArrayList<>();
            l.add("# questionId|examId|question|optionA|optionB|optionC|optionD|correctOption(1-4)|marks");
            l.add("Q1|E1|Which keyword is used to create an object in Java?|new|create|make|object|1|2");
            l.add("Q2|E1|Which of these is NOT a primitive data type?|int|boolean|String|char|3|2");
            l.add("Q3|E1|Which method is the entry point of a Java program?|start()|main()|run()|init()|2|2");
            l.add("Q4|E1|Which keyword is used for inheritance?|implements|extends|inherits|super|2|2");
            l.add("Q5|E1|What is the size of an int in Java?|8 bits|16 bits|32 bits|64 bits|3|2");
            l.add("Q6|E2|What does SQL stand for?|Structured Query Language|Simple Query Logic|Standard Question Language|Sequential Query List|1|2");
            l.add("Q7|E2|Which key uniquely identifies a row in a table?|Foreign key|Primary key|Candidate list|Index key|2|2");
            l.add("Q8|E2|Which command removes all rows but keeps the table structure?|DROP|DELETE TABLE|TRUNCATE|REMOVE|3|2");
            l.add("Q9|E2|Which normal form removes partial dependency?|1NF|2NF|3NF|BCNF|2|2");
            l.add("Q10|E2|Which SQL clause is used to filter rows?|ORDER BY|GROUP BY|WHERE|HAVING|3|2");
            l.add("Q11|E3|What is 15% of 200?|20|25|30|35|3|1");
            l.add("Q12|E3|Next number in series: 2, 4, 8, 16, ?|24|32|30|28|2|1");
            l.add("Q13|E3|A train travels 120 km in 2 hours. Its speed is?|50 km/h|60 km/h|70 km/h|80 km/h|2|1");
            l.add("Q14|E3|If 5 pens cost Rs. 50, how much do 8 pens cost?|Rs. 70|Rs. 75|Rs. 80|Rs. 90|3|1");
            l.add("Q15|E3|Find the odd one out: Apple, Mango, Potato, Banana|Apple|Mango|Potato|Banana|3|1");
            writeLines(QUESTIONS_FILE, l);
        }
        if (!Files.exists(RESULTS_FILE)) {
            List<String> l = new ArrayList<>();
            l.add("# studentId|examId|score|totalMarks|dateTime|passed");
            writeLines(RESULTS_FILE, l);
        }
    }

    static void writeLines(Path path, List<String> lines) throws IOException {
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static List<String[]> readRecords(Path path, int expectedFields) throws IOException {
        List<String[]> out = new ArrayList<>();
        int lineNo = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNo++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            if (p.length != expectedFields) {
                System.out.println("Warning: skipped bad line " + lineNo + " in " + path.getFileName());
                continue;
            }
            for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
            out.add(p);
        }
        return out;
    }

    static void loadAll() throws IOException {
        for (String[] p : readRecords(ADMIN_FILE, 2)) {
            adminUser = p[0];
            adminPass = p[1];
            break;
        }
        students.clear();
        for (String[] p : readRecords(STUDENTS_FILE, 4)) students.add(new Student(p[0], p[1], p[2], p[3]));
        exams.clear();
        for (String[] p : readRecords(EXAMS_FILE, 5)) {
            try {
                exams.add(new Exam(p[0], p[1], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4])));
            } catch (NumberFormatException e) {
                System.out.println("Warning: bad number in exams file for " + p[0]);
            }
        }
        questions.clear();
        for (String[] p : readRecords(QUESTIONS_FILE, 9)) {
            try {
                int correct = Integer.parseInt(p[7]);
                int marks = Integer.parseInt(p[8]);
                if (correct < 1 || correct > 4) throw new NumberFormatException();
                questions.add(new Question(p[0], p[1], p[2], new String[]{p[3], p[4], p[5], p[6]}, correct, marks));
            } catch (NumberFormatException e) {
                System.out.println("Warning: bad number in questions file for " + p[0]);
            }
        }
        results.clear();
        for (String[] p : readRecords(RESULTS_FILE, 6)) {
            try {
                results.add(new Result(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4],
                        Boolean.parseBoolean(p[5])));
            } catch (NumberFormatException e) {
                System.out.println("Warning: bad number in results file.");
            }
        }
    }

    static void saveAdmin() {
        List<String> l = new ArrayList<>();
        l.add("# username|password");
        l.add(adminUser + SEP + adminPass);
        safeWrite(ADMIN_FILE, l);
    }

    static void saveStudents() {
        List<String> l = new ArrayList<>();
        l.add("# studentId|name|password|course");
        for (Student s : students) l.add(s.toLine());
        safeWrite(STUDENTS_FILE, l);
    }

    static void saveExams() {
        List<String> l = new ArrayList<>();
        l.add("# examId|title|subject|durationMinutes|passPercent");
        for (Exam e : exams) l.add(e.toLine());
        safeWrite(EXAMS_FILE, l);
    }

    static void saveQuestions() {
        List<String> l = new ArrayList<>();
        l.add("# questionId|examId|question|optionA|optionB|optionC|optionD|correctOption(1-4)|marks");
        for (Question q : questions) l.add(q.toLine());
        safeWrite(QUESTIONS_FILE, l);
    }

    static void saveResults() {
        List<String> l = new ArrayList<>();
        l.add("# studentId|examId|score|totalMarks|dateTime|passed");
        for (Result r : results) l.add(r.toLine());
        safeWrite(RESULTS_FILE, l);
    }

    static void safeWrite(Path path, List<String> lines) {
        try {
            writeLines(path, lines);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not save " + path.getFileName() + ":\n" + e.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static String clean(String s) {
        return s.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }

    static Student findStudent(String id) {
        for (Student s : students) if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }

    static Exam findExam(String id) {
        for (Exam e : exams) if (e.id.equalsIgnoreCase(id)) return e;
        return null;
    }

    static Question findQuestion(String id) {
        for (Question q : questions) if (q.id.equalsIgnoreCase(id)) return q;
        return null;
    }

    static List<Question> questionsOf(String examId) {
        List<Question> l = new ArrayList<>();
        for (Question q : questions) if (q.examId.equalsIgnoreCase(examId)) l.add(q);
        return l;
    }

    static int totalMarks(String examId) {
        int t = 0;
        for (Question q : questionsOf(examId)) t += q.marks;
        return t;
    }

    static String nextId(String prefix, List<String> existing) {
        int max = 0;
        for (String id : existing) {
            if (id.toUpperCase().startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return prefix + (max + 1);
    }

    static String examTitle(String examId) {
        Exam e = findExam(examId);
        return e == null ? "(deleted)" : e.title;
    }

    static String studentName(String id) {
        Student s = findStudent(id);
        return s == null ? "(deleted)" : s.name;
    }

    // =====================================================================
    //                         THEME / UI HELPERS
    // =====================================================================
    static final Color NAVY = new Color(0x1F3A5F);
    static final Color NAVY_DARK = new Color(0x16294A);
    static final Color NAV_BTN = new Color(0x274A78);
    static final Color TEAL = new Color(0x2A9D8F);
    static final Color BLUE = new Color(0x3B82C4);
    static final Color BG = new Color(0xF2F5FA);
    static final Color WHITE = Color.WHITE;
    static final Color TEXT = new Color(0x2B2D42);
    static final Color GREY = new Color(0x8D99AE);
    static final Color RED = new Color(0xE63946);
    static final Color GREEN = new Color(0x2E9E5B);
    static final Color ORANGE = new Color(0xF4A261);
    static final Color PURPLE = new Color(0x7B5EA7);
    static final Color LINE = new Color(0xE0E5EE);

    static String fontName = "SansSerif";

    static Font font(int style, int size) {
        return new Font(fontName, style, size);
    }

    static Border pad(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    static Border inputBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xC5CEDB)), pad(6, 8, 6, 8));
    }

    static JTextField tf(String text) {
        JTextField t = new JTextField(text, 20);
        t.setFont(font(Font.PLAIN, 14));
        t.setBorder(inputBorder());
        return t;
    }

    static JPasswordField pf() {
        JPasswordField t = new JPasswordField(20);
        t.setFont(font(Font.PLAIN, 14));
        t.setBorder(inputBorder());
        return t;
    }

    static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(TEXT);
        return l;
    }

    static JSpinner spinner(int v, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(v, min, max, 1));
        s.setFont(font(Font.PLAIN, 14));
        return s;
    }

    static int val(JSpinner s) {
        try {
            s.commitEdit();
        } catch (ParseException ignored) {
        }
        return ((Number) s.getValue()).intValue();
    }

    static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(14, 14, 14, 14)));
        return p;
    }

    static JPanel toolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setBackground(WHITE);
        return p;
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(WHITE);
        return sp;
    }

    static void info(Component c, String m) {
        JOptionPane.showMessageDialog(c, m, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static void warn(Component c, String m) {
        JOptionPane.showMessageDialog(c, m, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    static boolean ask(Component c, String m) {
        return JOptionPane.showConfirmDialog(c, m, "Confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static void onChange(JTextField tf, Runnable r) {
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    /** Rounded coloured button with hover effect. */
    static class StyledButton extends JButton {
        private Color base;
        private boolean hover = false;

        StyledButton(String text, Color base) {
            super(text);
            this.base = base;
            setForeground(Color.WHITE);
            setFont(font(Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(pad(8, 18, 8, 18));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setBase(Color c) {
            base = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c;
            if (!isEnabled()) c = new Color(0xD5D9E0);
            else if (getModel().isPressed()) c = base.darker().darker();
            else if (hover) c = base.darker();
            else c = base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setFont(font(Font.PLAIN, 14));
        t.setGridColor(new Color(0xE9EDF4));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(0xCDEEEA));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 38));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r,
                                                           int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setOpaque(true);
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(font(Font.BOLD, 14));
                setBorder(pad(0, 10, 0, 10));
                return this;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r,
                                                           int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(pad(0, 10, 0, 10));
                setFont(font(Font.PLAIN, 14));
                String s = v == null ? "" : v.toString();
                if (!sel) {
                    setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF6F8FC));
                    setForeground(TEXT);
                }
                if (s.equals("PASS")) {
                    setFont(font(Font.BOLD, 14));
                    if (!sel) setForeground(GREEN);
                } else if (s.equals("FAIL")) {
                    setFont(font(Font.BOLD, 14));
                    if (!sel) setForeground(RED);
                }
                return this;
            }
        });
        return t;
    }

    static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    /** Returns the first-column value of the selected row, or null (with a message). */
    static String selectedKey(Component parent, JTable t, String what) {
        int r = t.getSelectedRow();
        if (r < 0) {
            warn(parent, "Please select a " + what + " from the table first.");
            return null;
        }
        return String.valueOf(t.getValueAt(r, 0));
    }

    static JPanel formPanel(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < labels.length; i++) {
            gc.gridy = i;
            gc.gridx = 0;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            p.add(label(labels[i]), gc);
            gc.gridx = 1;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            p.add(fields[i], gc);
        }
        return p;
    }

    /** Shows a form in a dialog; keeps re-showing until validator returns null or user cancels. */
    static boolean formDialog(Component parent, String title, String[] labels, JComponent[] fields,
                              Supplier<String> validator) {
        JPanel p = formPanel(labels, fields);
        while (true) {
            int r = JOptionPane.showConfirmDialog(parent, p, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return false;
            String err = validator.get();
            if (err == null) return true;
            warn(parent, err);
        }
    }

    static JPanel header(String title, String user, Runnable logout) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(NAVY);
        h.setBorder(pad(12, 20, 12, 20));
        JLabel t = new JLabel(title);
        t.setForeground(WHITE);
        t.setFont(font(Font.BOLD, 20));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        JLabel u = new JLabel(user);
        u.setForeground(new Color(0xCFE3F5));
        u.setFont(font(Font.PLAIN, 14));
        StyledButton lo = new StyledButton("Logout", RED);
        lo.addActionListener(e -> {
            if (ask(frame, "Do you want to log out?")) logout.run();
        });
        right.add(u);
        right.add(lo);
        h.add(t, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    static class NavBar extends JPanel {
        private final List<StyledButton> buttons = new ArrayList<>();

        NavBar() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(NAVY_DARK);
            setBorder(pad(20, 12, 20, 12));
            setPreferredSize(new Dimension(210, 0));
        }

        void addItem(String text, Runnable action) {
            final StyledButton b = new StyledButton(text, NAV_BTN);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.addActionListener(e -> {
                select(b);
                action.run();
            });
            buttons.add(b);
            add(b);
            add(Box.createVerticalStrut(8));
        }

        void select(StyledButton sel) {
            for (StyledButton b : buttons) b.setBase(b == sel ? TEAL : NAV_BTN);
        }

        void click(int index) {
            if (index >= 0 && index < buttons.size()) buttons.get(index).doClick();
        }
    }

    static abstract class Page extends JPanel {
        Page(String title, String subtitle) {
            setLayout(new BorderLayout(0, 14));
            setBackground(BG);
            setBorder(pad(20, 24, 20, 24));
            JPanel top = new JPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.setOpaque(false);
            JLabel t = new JLabel(title);
            t.setFont(font(Font.BOLD, 26));
            t.setForeground(NAVY);
            JLabel s = new JLabel(subtitle);
            s.setFont(font(Font.PLAIN, 14));
            s.setForeground(GREY);
            top.add(t);
            top.add(Box.createVerticalStrut(2));
            top.add(s);
            add(top, BorderLayout.NORTH);
        }

        abstract void refresh();
    }

    static class StatCard extends JPanel {
        private final Color color;
        private final JLabel value = new JLabel("0");

        StatCard(String title, Color color) {
            this.color = color;
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(pad(14, 20, 14, 20));
            value.setFont(font(Font.BOLD, 34));
            value.setForeground(WHITE);
            JLabel t = new JLabel(title);
            t.setFont(font(Font.PLAIN, 14));
            t.setForeground(new Color(0xF0F6FF));
            add(value, BorderLayout.CENTER);
            add(t, BorderLayout.SOUTH);
        }

        void setValue(String v) {
            value.setText(v);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    //                              MAIN WINDOW
    // =====================================================================
    static JFrame frame;
    static final CardLayout rootLayout = new CardLayout();
    static final JPanel root = new JPanel(rootLayout);
    static LoginPanel loginPanel;
    static AdminPanel adminPanel;
    static StudentPanel studentPanel;
    static ExamPanel examPanel;
    static boolean examActive = false;

    public static void main(String[] args) {
        try {
            createDefaultFilesIfMissing();
            loadAll();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not read/write data files:\n" + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(ExamManagementGUI::startUi);
    }

    static void startUi() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        for (String fam : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (fam.equals("Segoe UI")) {
                fontName = "Segoe UI";
                break;
            }
        }
        FontUIResource fr = new FontUIResource(font(Font.PLAIN, 14));
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (UIManager.get(k) instanceof FontUIResource) UIManager.put(k, fr);
        }
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);

        frame = new JFrame("Exam Management System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (examActive) {
                    if (!ask(frame, "An exam is in progress. If you quit now, this attempt will NOT be saved.\nQuit anyway?"))
                        return;
                }
                System.exit(0);
            }
        });
        root.setBackground(BG);
        loginPanel = new LoginPanel();
        root.add(loginPanel, "LOGIN");
        frame.setContentPane(root);
        frame.setSize(1150, 720);
        frame.setMinimumSize(new Dimension(980, 640));
        frame.setLocationRelativeTo(null);
        showLogin();
        frame.setVisible(true);
    }

    static void showLogin() {
        if (adminPanel != null) {
            root.remove(adminPanel);
            adminPanel = null;
        }
        if (studentPanel != null) {
            root.remove(studentPanel);
            studentPanel = null;
        }
        loginPanel.reset();
        rootLayout.show(root, "LOGIN");
        frame.getRootPane().setDefaultButton(loginPanel.loginBtn);
    }

    static void showAdmin() {
        adminPanel = new AdminPanel();
        root.add(adminPanel, "ADMIN");
        rootLayout.show(root, "ADMIN");
        frame.getRootPane().setDefaultButton(null);
    }

    static void showStudent(Student s) {
        studentPanel = new StudentPanel(s);
        root.add(studentPanel, "STUDENT");
        rootLayout.show(root, "STUDENT");
        frame.getRootPane().setDefaultButton(null);
    }

    // =====================================================================
    //                              LOGIN
    // =====================================================================
    static class LoginPanel extends JPanel {
        final JRadioButton adminRb = new JRadioButton("Admin");
        final JRadioButton studentRb = new JRadioButton("Student", true);
        final JTextField idField = tf("");
        final JPasswordField pwField = pf();
        final JLabel idLabel = label("Student ID");
        final JLabel msg = new JLabel(" ");
        final StyledButton loginBtn = new StyledButton("LOGIN", TEAL);

        LoginPanel() {
            setLayout(new GridLayout(1, 2));
            add(buildBanner());
            add(buildForm());
        }

        private JPanel buildBanner() {
            JPanel banner = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(0, 0, NAVY, 0, getHeight(), new Color(0x2A6F97)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(4, 50, 4, 30);

            JLabel big = new JLabel("EXAM");
            big.setFont(font(Font.BOLD, 64));
            big.setForeground(WHITE);
            JLabel sub = new JLabel("MANAGEMENT SYSTEM");
            sub.setFont(font(Font.BOLD, 24));
            sub.setForeground(new Color(0x9EE6DC));
            JPanel accent = new JPanel();
            accent.setBackground(TEAL);
            accent.setPreferredSize(new Dimension(90, 5));
            JLabel tag = new JLabel("Create, conduct and evaluate exams with ease.");
            tag.setFont(font(Font.PLAIN, 15));
            tag.setForeground(new Color(0xD6E6F5));

            gc.gridy = 0;
            banner.add(big, gc);
            gc.gridy = 1;
            banner.add(sub, gc);
            gc.gridy = 2;
            gc.insets = new Insets(14, 50, 14, 30);
            banner.add(accent, gc);
            gc.insets = new Insets(4, 50, 4, 30);
            gc.gridy = 3;
            banner.add(tag, gc);
            String[] feats = {"  \u2022  Manage students, exams and questions", "  \u2022  Timed online tests with auto-submit",
                    "  \u2022  Instant results and reports", "  \u2022  All data saved automatically"};
            gc.insets = new Insets(2, 50, 2, 30);
            for (int i = 0; i < feats.length; i++) {
                JLabel l = new JLabel(feats[i]);
                l.setFont(font(Font.PLAIN, 14));
                l.setForeground(new Color(0xE8F1FA));
                gc.gridy = 4 + i;
                if (i == 0) gc.insets = new Insets(24, 50, 2, 30);
                else gc.insets = new Insets(2, 50, 2, 30);
                banner.add(l, gc);
            }
            return banner;
        }

        private JPanel buildForm() {
            JPanel outer = new JPanel(new GridBagLayout());
            outer.setBackground(WHITE);
            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            gc.insets = new Insets(5, 0, 5, 0);

            JLabel title = new JLabel("Welcome Back");
            title.setFont(font(Font.BOLD, 30));
            title.setForeground(NAVY);
            JLabel sub = new JLabel("Sign in to continue");
            sub.setFont(font(Font.PLAIN, 14));
            sub.setForeground(GREY);

            ButtonGroup bg = new ButtonGroup();
            bg.add(adminRb);
            bg.add(studentRb);
            for (JRadioButton rb : new JRadioButton[]{studentRb, adminRb}) {
                rb.setBackground(WHITE);
                rb.setFont(font(Font.BOLD, 14));
                rb.setForeground(NAVY);
                rb.setFocusPainted(false);
            }
            studentRb.addActionListener(e -> {
                idLabel.setText("Student ID");
                msg.setText(" ");
            });
            adminRb.addActionListener(e -> {
                idLabel.setText("Admin Username");
                msg.setText(" ");
            });
            JPanel roles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            roles.setBackground(WHITE);
            roles.add(studentRb);
            roles.add(Box.createHorizontalStrut(18));
            roles.add(adminRb);

            msg.setForeground(RED);
            msg.setFont(font(Font.BOLD, 13));
            loginBtn.setFont(font(Font.BOLD, 16));
            loginBtn.addActionListener(e -> doLogin());

            JLabel hint = new JLabel("<html><body style='width:300px'>Demo: admin / admin123 &nbsp;|&nbsp; S101 / pass101</body></html>");
            hint.setFont(font(Font.PLAIN, 12));
            hint.setForeground(GREY);

            int y = 0;
            gc.gridy = y++;
            form.add(title, gc);
            gc.gridy = y++;
            form.add(sub, gc);
            gc.gridy = y++;
            gc.insets = new Insets(18, 0, 5, 0);
            form.add(roles, gc);
            gc.insets = new Insets(5, 0, 5, 0);
            gc.gridy = y++;
            form.add(idLabel, gc);
            gc.gridy = y++;
            form.add(idField, gc);
            gc.gridy = y++;
            form.add(label("Password"), gc);
            gc.gridy = y++;
            form.add(pwField, gc);
            gc.gridy = y++;
            form.add(msg, gc);
            gc.gridy = y++;
            form.add(loginBtn, gc);
            gc.gridy = y++;
            gc.insets = new Insets(14, 0, 5, 0);
            form.add(hint, gc);

            form.setPreferredSize(new Dimension(360, 460));
            outer.add(form);
            return outer;
        }

        void doLogin() {
            String id = idField.getText().trim();
            String pw = new String(pwField.getPassword());
            if (id.isEmpty() || pw.isEmpty()) {
                msg.setText("Please enter both fields.");
                return;
            }
            if (adminRb.isSelected()) {
                if (id.equals(adminUser) && pw.equals(adminPass)) showAdmin();
                else msg.setText("Invalid admin username or password.");
            } else {
                Student s = findStudent(id);
                if (s != null && s.password.equals(pw)) showStudent(s);
                else msg.setText("Invalid student ID or password.");
            }
        }

        void reset() {
            idField.setText("");
            pwField.setText("");
            msg.setText(" ");
            idField.requestFocusInWindow();
        }
    }

    // =====================================================================
    //                              ADMIN
    // =====================================================================
    static class AdminPanel extends JPanel {
        final CardLayout cl = new CardLayout();
        final JPanel content = new JPanel(cl);
        final List<Page> pages = new ArrayList<>();
        final NavBar nav = new NavBar();

        AdminPanel() {
            setLayout(new BorderLayout());
            add(header("Exam Management System  |  Admin Panel", "Logged in as: " + adminUser,
                    ExamManagementGUI::showLogin), BorderLayout.NORTH);
            content.setBackground(BG);
            addPage("Dashboard", new DashboardPanel());
            addPage("Students", new StudentsPanel());
            addPage("Exams", new ExamsPanel());
            addPage("Questions", new QuestionsPanel());
            addPage("Results", new ResultsPanel());
            addPage("Settings", new AdminSettingsPanel(this::refreshAll));
            add(nav, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
            nav.click(0);
        }

        void addPage(String name, Page p) {
            pages.add(p);
            content.add(p, name);
            nav.addItem(name, () -> {
                p.refresh();
                cl.show(content, name);
            });
        }

        void refreshAll() {
            for (Page p : pages) p.refresh();
        }
    }

    // ---------- Dashboard ----------
    static class DashboardPanel extends Page {
        final StatCard cStudents = new StatCard("Students", BLUE);
        final StatCard cExams = new StatCard("Exams", TEAL);
        final StatCard cQuestions = new StatCard("Questions", PURPLE);
        final StatCard cAttempts = new StatCard("Exam Attempts", ORANGE);
        final DefaultTableModel m = model("Exam", "Attempts", "Average %", "Highest %", "Passed", "Pass Rate");
        final JTable t = table(m);
        final JLabel top = new JLabel(" ");

        DashboardPanel() {
            super("Dashboard", "Overview and exam-wise statistics");
            JPanel body = new JPanel(new BorderLayout(0, 16));
            body.setOpaque(false);
            JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
            cards.setOpaque(false);
            cards.setPreferredSize(new Dimension(100, 108));
            cards.add(cStudents);
            cards.add(cExams);
            cards.add(cQuestions);
            cards.add(cAttempts);
            JPanel c = card();
            JLabel h = new JLabel("Exam-wise Statistics");
            h.setFont(font(Font.BOLD, 17));
            h.setForeground(NAVY);
            top.setFont(font(Font.BOLD, 14));
            top.setForeground(GREEN);
            c.add(h, BorderLayout.NORTH);
            c.add(scroll(t), BorderLayout.CENTER);
            c.add(top, BorderLayout.SOUTH);
            body.add(cards, BorderLayout.NORTH);
            body.add(c, BorderLayout.CENTER);
            add(body, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            cStudents.setValue(String.valueOf(students.size()));
            cExams.setValue(String.valueOf(exams.size()));
            cQuestions.setValue(String.valueOf(questions.size()));
            cAttempts.setValue(String.valueOf(results.size()));
            m.setRowCount(0);
            for (Exam e : exams) {
                int n = 0, pass = 0;
                double sum = 0, high = 0;
                for (Result r : results) {
                    if (r.examId.equalsIgnoreCase(e.id)) {
                        n++;
                        sum += r.percent();
                        high = Math.max(high, r.percent());
                        if (r.passed) pass++;
                    }
                }
                if (n == 0) m.addRow(new Object[]{e.title, "0", "-", "-", "-", "-"});
                else m.addRow(new Object[]{e.title, String.valueOf(n), String.format("%.1f", sum / n),
                        String.format("%.1f", high), pass + " / " + n, String.format("%.0f%%", pass * 100.0 / n)});
            }
            Result best = null;
            for (Result r : results) if (best == null || r.percent() > best.percent()) best = r;
            if (best == null) top.setText("No exam attempts yet.");
            else top.setText(String.format("Top performance: %s in '%s' with %.1f%% (%d/%d)",
                    studentName(best.studentId), examTitle(best.examId), best.percent(), best.score, best.total));
        }
    }

    // ---------- Students ----------
    static class StudentsPanel extends Page {
        final DefaultTableModel m = model("Student ID", "Name", "Password", "Course");
        final JTable t = table(m);
        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        final JTextField search = tf("");

        StudentsPanel() {
            super("Students", "Add, edit or remove students. Every change is saved instantly.");
            t.setRowSorter(sorter);
            widths(t, 90, 200, 120, 260);
            JPanel c = card();
            JPanel bar = toolbar();
            search.setColumns(16);
            bar.add(label("Search:"));
            bar.add(search);
            StyledButton add = new StyledButton("+ Add Student", TEAL);
            StyledButton edit = new StyledButton("Edit", BLUE);
            StyledButton del = new StyledButton("Delete", RED);
            add.addActionListener(e -> addStudent());
            edit.addActionListener(e -> editStudent());
            del.addActionListener(e -> deleteStudent());
            bar.add(add);
            bar.add(edit);
            bar.add(del);
            onChange(search, () -> {
                String txt = search.getText().trim();
                if (txt.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txt)));
            });
            c.add(bar, BorderLayout.NORTH);
            c.add(scroll(t), BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            m.setRowCount(0);
            for (Student s : students) m.addRow(new Object[]{s.id, s.name, s.password, s.course});
        }

        void addStudent() {
            JTextField id = tf(""), name = tf(""), pw = tf(""), course = tf("");
            boolean ok = formDialog(this, "Add Student", new String[]{"Student ID", "Name", "Password", "Course"},
                    new JComponent[]{id, name, pw, course}, () -> {
                        String sid = clean(id.getText()).toUpperCase().replace(" ", "");
                        if (sid.isEmpty()) return "Student ID is required.";
                        if (findStudent(sid) != null) return "That Student ID already exists.";
                        if (clean(name.getText()).isEmpty() || clean(pw.getText()).isEmpty()
                                || clean(course.getText()).isEmpty()) return "Name, password and course are required.";
                        return null;
                    });
            if (!ok) return;
            students.add(new Student(clean(id.getText()).toUpperCase().replace(" ", ""), clean(name.getText()),
                    clean(pw.getText()), clean(course.getText())));
            saveStudents();
            refresh();
        }

        void editStudent() {
            String key = selectedKey(this, t, "student");
            if (key == null) return;
            Student s = findStudent(key);
            if (s == null) return;
            JTextField id = tf(s.id), name = tf(s.name), pw = tf(s.password), course = tf(s.course);
            id.setEditable(false);
            id.setBackground(new Color(0xEEF1F6));
            boolean ok = formDialog(this, "Edit Student", new String[]{"Student ID", "Name", "Password", "Course"},
                    new JComponent[]{id, name, pw, course}, () -> {
                        if (clean(name.getText()).isEmpty() || clean(pw.getText()).isEmpty()
                                || clean(course.getText()).isEmpty()) return "Name, password and course are required.";
                        return null;
                    });
            if (!ok) return;
            s.name = clean(name.getText());
            s.password = clean(pw.getText());
            s.course = clean(course.getText());
            saveStudents();
            refresh();
        }

        void deleteStudent() {
            String key = selectedKey(this, t, "student");
            if (key == null) return;
            Student s = findStudent(key);
            if (s == null) return;
            if (!ask(this, "Delete " + s.name + " and all their exam results?")) return;
            students.remove(s);
            final String sid = s.id;
            results.removeIf(r -> r.studentId.equalsIgnoreCase(sid));
            saveStudents();
            saveResults();
            refresh();
        }
    }

    // ---------- Exams ----------
    static class ExamsPanel extends Page {
        final DefaultTableModel m = model("Exam ID", "Title", "Subject", "Minutes", "Pass %", "Questions", "Total Marks");
        final JTable t = table(m);

        ExamsPanel() {
            super("Exams", "Create and manage exams. Add their questions from the Questions page.");
            widths(t, 70, 250, 150, 80, 70, 90, 100);
            JPanel c = card();
            JPanel bar = toolbar();
            StyledButton add = new StyledButton("+ Add Exam", TEAL);
            StyledButton edit = new StyledButton("Edit", BLUE);
            StyledButton del = new StyledButton("Delete", RED);
            add.addActionListener(e -> addExam());
            edit.addActionListener(e -> editExam());
            del.addActionListener(e -> deleteExam());
            bar.add(add);
            bar.add(edit);
            bar.add(del);
            c.add(bar, BorderLayout.NORTH);
            c.add(scroll(t), BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            m.setRowCount(0);
            for (Exam e : exams) {
                m.addRow(new Object[]{e.id, e.title, e.subject, e.durationMin, e.passPercent,
                        questionsOf(e.id).size(), totalMarks(e.id)});
            }
        }

        void addExam() {
            JTextField title = tf(""), subject = tf("");
            JSpinner dur = spinner(10, 1, 300), pass = spinner(40, 1, 100);
            boolean ok = formDialog(this, "Add Exam",
                    new String[]{"Title", "Subject", "Duration (minutes)", "Pass percentage"},
                    new JComponent[]{title, subject, dur, pass}, () -> {
                        if (clean(title.getText()).isEmpty() || clean(subject.getText()).isEmpty())
                            return "Title and subject are required.";
                        return null;
                    });
            if (!ok) return;
            List<String> ids = new ArrayList<>();
            for (Exam e : exams) ids.add(e.id);
            exams.add(new Exam(nextId("E", ids), clean(title.getText()), clean(subject.getText()), val(dur),
                    val(pass)));
            saveExams();
            refresh();
        }

        void editExam() {
            String key = selectedKey(this, t, "exam");
            if (key == null) return;
            Exam e = findExam(key);
            if (e == null) return;
            JTextField title = tf(e.title), subject = tf(e.subject);
            JSpinner dur = spinner(e.durationMin, 1, 300), pass = spinner(e.passPercent, 1, 100);
            boolean ok = formDialog(this, "Edit Exam " + e.id,
                    new String[]{"Title", "Subject", "Duration (minutes)", "Pass percentage"},
                    new JComponent[]{title, subject, dur, pass}, () -> {
                        if (clean(title.getText()).isEmpty() || clean(subject.getText()).isEmpty())
                            return "Title and subject are required.";
                        return null;
                    });
            if (!ok) return;
            e.title = clean(title.getText());
            e.subject = clean(subject.getText());
            e.durationMin = val(dur);
            e.passPercent = val(pass);
            saveExams();
            refresh();
        }

        void deleteExam() {
            String key = selectedKey(this, t, "exam");
            if (key == null) return;
            Exam e = findExam(key);
            if (e == null) return;
            if (!ask(this, "Delete '" + e.title + "' together with all its questions and results?")) return;
            final String eid = e.id;
            exams.remove(e);
            questions.removeIf(q -> q.examId.equalsIgnoreCase(eid));
            results.removeIf(r -> r.examId.equalsIgnoreCase(eid));
            saveExams();
            saveQuestions();
            saveResults();
            refresh();
        }
    }

    // ---------- Questions ----------
    static class QuestionsPanel extends Page {
        final DefaultTableModel m = model("ID", "Question", "A", "B", "C", "D", "Correct", "Marks");
        final JTable t = table(m);
        final JComboBox<String> examBox = new JComboBox<>();
        final List<String> examIds = new ArrayList<>();
        boolean updating = false;

        QuestionsPanel() {
            super("Questions", "Choose an exam, then add, edit or delete its questions.");
            widths(t, 55, 330, 120, 120, 120, 120, 65, 60);
            examBox.setFont(font(Font.PLAIN, 14));
            examBox.setPreferredSize(new Dimension(320, 34));
            examBox.addActionListener(e -> {
                if (!updating) loadTable();
            });
            JPanel c = card();
            JPanel bar = toolbar();
            bar.add(label("Exam:"));
            bar.add(examBox);
            StyledButton add = new StyledButton("+ Add Question", TEAL);
            StyledButton edit = new StyledButton("Edit", BLUE);
            StyledButton del = new StyledButton("Delete", RED);
            add.addActionListener(e -> addQuestion());
            edit.addActionListener(e -> editQuestion());
            del.addActionListener(e -> deleteQuestion());
            bar.add(add);
            bar.add(edit);
            bar.add(del);
            c.add(bar, BorderLayout.NORTH);
            JScrollPane sp = scroll(t);
            c.add(sp, BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        String currentExamId() {
            int i = examBox.getSelectedIndex();
            return (i < 0 || i >= examIds.size()) ? null : examIds.get(i);
        }

        @Override
        void refresh() {
            String prev = currentExamId();
            updating = true;
            examBox.removeAllItems();
            examIds.clear();
            for (Exam e : exams) {
                examBox.addItem(e.id + " - " + e.title);
                examIds.add(e.id);
            }
            int idx = prev == null ? -1 : examIds.indexOf(prev);
            if (examBox.getItemCount() > 0) examBox.setSelectedIndex(idx >= 0 ? idx : 0);
            updating = false;
            loadTable();
        }

        void loadTable() {
            m.setRowCount(0);
            String eid = currentExamId();
            if (eid == null) return;
            for (Question q : questionsOf(eid)) {
                m.addRow(new Object[]{q.id, q.text, q.options[0], q.options[1], q.options[2], q.options[3],
                        String.valueOf((char) ('A' + q.correct - 1)), q.marks});
            }
        }

        private JComponent[] buildFields(JTextArea qa, JTextField[] opts, JComboBox<String> correct, JSpinner marks) {
            JScrollPane qsp = new JScrollPane(qa);
            qsp.setBorder(BorderFactory.createLineBorder(new Color(0xC5CEDB)));
            return new JComponent[]{qsp, opts[0], opts[1], opts[2], opts[3], correct, marks};
        }

        private final String[] LABELS = {"Question", "Option A", "Option B", "Option C", "Option D",
                "Correct answer", "Marks"};

        private JTextArea area(String text) {
            JTextArea a = new JTextArea(text, 3, 28);
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
            a.setFont(font(Font.PLAIN, 14));
            a.setBorder(pad(4, 6, 4, 6));
            return a;
        }

        private String check(JTextArea qa, JTextField[] opts) {
            if (clean(qa.getText()).isEmpty()) return "Question text is required.";
            for (JTextField o : opts) if (clean(o.getText()).isEmpty()) return "All four options are required.";
            return null;
        }

        void addQuestion() {
            String eid = currentExamId();
            if (eid == null) {
                warn(this, "Please create an exam first (Exams page).");
                return;
            }
            JTextArea qa = area("");
            JTextField[] opts = {tf(""), tf(""), tf(""), tf("")};
            JComboBox<String> correct = new JComboBox<>(new String[]{"A", "B", "C", "D"});
            JSpinner marks = spinner(1, 1, 100);
            boolean ok = formDialog(this, "Add Question", LABELS, buildFields(qa, opts, correct, marks),
                    () -> check(qa, opts));
            if (!ok) return;
            List<String> ids = new ArrayList<>();
            for (Question q : questions) ids.add(q.id);
            questions.add(new Question(nextId("Q", ids), eid, clean(qa.getText()),
                    new String[]{clean(opts[0].getText()), clean(opts[1].getText()), clean(opts[2].getText()),
                            clean(opts[3].getText())}, correct.getSelectedIndex() + 1, val(marks)));
            saveQuestions();
            loadTable();
        }

        void editQuestion() {
            String key = selectedKey(this, t, "question");
            if (key == null) return;
            Question q = findQuestion(key);
            if (q == null) return;
            JTextArea qa = area(q.text);
            JTextField[] opts = {tf(q.options[0]), tf(q.options[1]), tf(q.options[2]), tf(q.options[3])};
            JComboBox<String> correct = new JComboBox<>(new String[]{"A", "B", "C", "D"});
            correct.setSelectedIndex(q.correct - 1);
            JSpinner marks = spinner(q.marks, 1, 100);
            boolean ok = formDialog(this, "Edit Question " + q.id, LABELS, buildFields(qa, opts, correct, marks),
                    () -> check(qa, opts));
            if (!ok) return;
            q.text = clean(qa.getText());
            for (int i = 0; i < 4; i++) q.options[i] = clean(opts[i].getText());
            q.correct = correct.getSelectedIndex() + 1;
            q.marks = val(marks);
            saveQuestions();
            loadTable();
        }

        void deleteQuestion() {
            String key = selectedKey(this, t, "question");
            if (key == null) return;
            Question q = findQuestion(key);
            if (q == null) return;
            if (!ask(this, "Delete question " + q.id + "?")) return;
            questions.remove(q);
            saveQuestions();
            loadTable();
        }
    }

    // ---------- Results ----------
    static class ResultsPanel extends Page {
        final DefaultTableModel m = model("Student ID", "Student", "Exam", "Score", "Percent", "Result", "Date");
        final JTable t = table(m);
        final JComboBox<String> filter = new JComboBox<>();
        final List<String> filterIds = new ArrayList<>();
        final List<Result> shown = new ArrayList<>();
        final JLabel count = new JLabel(" ");
        boolean updating = false;

        ResultsPanel() {
            super("Results", "All exam attempts by students.");
            widths(t, 90, 160, 220, 90, 80, 70, 140);
            filter.setFont(font(Font.PLAIN, 14));
            filter.setPreferredSize(new Dimension(280, 34));
            filter.addActionListener(e -> {
                if (!updating) load();
            });
            JPanel c = card();
            JPanel bar = toolbar();
            bar.add(label("Show:"));
            bar.add(filter);
            StyledButton del = new StyledButton("Delete Selected Result", RED);
            del.addActionListener(e -> deleteResult());
            bar.add(del);
            count.setFont(font(Font.BOLD, 13));
            count.setForeground(GREY);
            c.add(bar, BorderLayout.NORTH);
            c.add(scroll(t), BorderLayout.CENTER);
            c.add(count, BorderLayout.SOUTH);
            add(c, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            int prev = filter.getSelectedIndex();
            updating = true;
            filter.removeAllItems();
            filterIds.clear();
            filter.addItem("All Exams");
            filterIds.add(null);
            for (Exam e : exams) {
                filter.addItem(e.id + " - " + e.title);
                filterIds.add(e.id);
            }
            filter.setSelectedIndex(prev >= 0 && prev < filter.getItemCount() ? prev : 0);
            updating = false;
            load();
        }

        void load() {
            m.setRowCount(0);
            shown.clear();
            int i = filter.getSelectedIndex();
            String eid = (i >= 0 && i < filterIds.size()) ? filterIds.get(i) : null;
            for (Result r : results) {
                if (eid != null && !r.examId.equalsIgnoreCase(eid)) continue;
                shown.add(r);
                m.addRow(new Object[]{r.studentId, studentName(r.studentId), examTitle(r.examId),
                        r.score + " / " + r.total, String.format("%.1f%%", r.percent()), r.passed ? "PASS" : "FAIL",
                        r.dateTime});
            }
            count.setText(shown.size() + " record(s)");
        }

        void deleteResult() {
            int r = t.getSelectedRow();
            if (r < 0) {
                warn(this, "Please select a result row first.");
                return;
            }
            if (!ask(this, "Delete the selected result?")) return;
            results.remove(shown.get(r));
            saveResults();
            load();
        }
    }

    // ---------- Settings ----------
    static class PasswordCard extends JPanel {
        PasswordCard(Supplier<String> current, Consumer<String> setter) {
            setLayout(new BorderLayout(0, 10));
            setBackground(WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(16, 16, 16, 16)));
            JLabel title = new JLabel("Change Password");
            title.setFont(font(Font.BOLD, 17));
            title.setForeground(NAVY);
            JPasswordField cur = pf(), nw = pf(), conf = pf();
            JLabel msg = new JLabel(" ");
            msg.setFont(font(Font.BOLD, 13));
            JPanel form = formPanel(new String[]{"Current password", "New password", "Confirm new password"},
                    new JComponent[]{cur, nw, conf});
            StyledButton save = new StyledButton("Update Password", TEAL);
            save.addActionListener(e -> {
                String c = new String(cur.getPassword());
                String n = new String(nw.getPassword());
                String cf = new String(conf.getPassword());
                msg.setForeground(RED);
                if (!c.equals(current.get())) {
                    msg.setText("Current password is incorrect.");
                    return;
                }
                if (n.trim().isEmpty()) {
                    msg.setText("New password cannot be empty.");
                    return;
                }
                if (n.contains("|")) {
                    msg.setText("The '|' character is not allowed.");
                    return;
                }
                if (!n.equals(cf)) {
                    msg.setText("New passwords do not match.");
                    return;
                }
                setter.accept(n.trim());
                cur.setText("");
                nw.setText("");
                conf.setText("");
                msg.setForeground(GREEN);
                msg.setText("Password updated and saved.");
            });
            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            south.setOpaque(false);
            south.add(save);
            south.add(msg);
            add(title, BorderLayout.NORTH);
            add(form, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
        }
    }

    static class AdminSettingsPanel extends Page {
        AdminSettingsPanel(Runnable onReset) {
            super("Settings", "Change the admin password or restore the preloaded data.");
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);

            PasswordCard pc = new PasswordCard(() -> adminPass, np -> {
                adminPass = np;
                saveAdmin();
            });
            pc.setAlignmentX(Component.LEFT_ALIGNMENT);
            pc.setMaximumSize(new Dimension(560, 330));

            JPanel danger = new JPanel(new BorderLayout(0, 10));
            danger.setBackground(WHITE);
            danger.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xF3B6BB)),
                    pad(16, 16, 16, 16)));
            JLabel dt = new JLabel("Danger Zone");
            dt.setFont(font(Font.BOLD, 17));
            dt.setForeground(RED);
            JLabel dd = new JLabel("<html>Restores the original preloaded students, exams and questions and removes all results.</html>");
            dd.setFont(font(Font.PLAIN, 13));
            dd.setForeground(GREY);
            StyledButton reset = new StyledButton("Reset All Data", RED);
            reset.addActionListener(e -> {
                String s = JOptionPane.showInputDialog(this,
                        "This will DELETE all your changes and restore the preloaded data.\nType YES to continue:");
                if (!"YES".equals(s)) return;
                try {
                    Files.deleteIfExists(ADMIN_FILE);
                    Files.deleteIfExists(STUDENTS_FILE);
                    Files.deleteIfExists(EXAMS_FILE);
                    Files.deleteIfExists(QUESTIONS_FILE);
                    Files.deleteIfExists(RESULTS_FILE);
                    createDefaultFilesIfMissing();
                    loadAll();
                    onReset.run();
                    info(this, "All data has been restored to the defaults.");
                } catch (IOException ex) {
                    warn(this, "Reset failed: " + ex.getMessage());
                }
            });
            JPanel rb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            rb.setOpaque(false);
            rb.add(reset);
            danger.add(dt, BorderLayout.NORTH);
            danger.add(dd, BorderLayout.CENTER);
            danger.add(rb, BorderLayout.SOUTH);
            danger.setAlignmentX(Component.LEFT_ALIGNMENT);
            danger.setMaximumSize(new Dimension(560, 160));

            body.add(pc);
            body.add(Box.createVerticalStrut(18));
            body.add(danger);
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(body, BorderLayout.NORTH);
            add(wrap, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
        }
    }

    // =====================================================================
    //                              STUDENT
    // =====================================================================
    static class StudentPanel extends JPanel {
        final Student student;
        final CardLayout cl = new CardLayout();
        final JPanel content = new JPanel(cl);
        final NavBar nav = new NavBar();

        StudentPanel(Student s) {
            this.student = s;
            setLayout(new BorderLayout());
            add(header("Exam Management System  |  Student Portal", s.name + "  (" + s.id + ")",
                    ExamManagementGUI::showLogin), BorderLayout.NORTH);
            content.setBackground(BG);
            addPage("Take Exam", new TakeExamPage(s));
            addPage("My Results", new MyResultsPage(s));
            addPage("Change Password", new StudentPasswordPage(s));
            add(nav, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
            nav.click(0);
        }

        void addPage(String name, Page p) {
            content.add(p, name);
            nav.addItem(name, () -> {
                p.refresh();
                cl.show(content, name);
            });
        }
    }

    static class TakeExamPage extends Page {
        final Student student;
        final DefaultTableModel m = model("Exam", "Subject", "Duration", "Questions", "Total Marks", "Pass %");
        final JTable t = table(m);
        final List<Exam> avail = new ArrayList<>();

        TakeExamPage(Student s) {
            super("Take an Exam", "Select an exam and press Start. The timer begins immediately.");
            this.student = s;
            widths(t, 260, 150, 90, 90, 100, 70);
            JPanel c = card();
            JPanel bar = toolbar();
            StyledButton start = new StyledButton("Start Selected Exam  \u25B6", TEAL);
            start.setFont(font(Font.BOLD, 15));
            start.addActionListener(e -> start());
            bar.add(start);
            JLabel tip = new JLabel("Tip: each question can be changed before you submit. The exam auto-submits when time is up.");
            tip.setFont(font(Font.PLAIN, 12));
            tip.setForeground(GREY);
            c.add(bar, BorderLayout.NORTH);
            c.add(scroll(t), BorderLayout.CENTER);
            c.add(tip, BorderLayout.SOUTH);
            add(c, BorderLayout.CENTER);
            t.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) start();
                }
            });
        }

        @Override
        void refresh() {
            m.setRowCount(0);
            avail.clear();
            for (Exam e : exams) {
                if (questionsOf(e.id).isEmpty()) continue;
                avail.add(e);
                m.addRow(new Object[]{e.title, e.subject, e.durationMin + " min", questionsOf(e.id).size(),
                        totalMarks(e.id), e.passPercent + "%"});
            }
        }

        void start() {
            int r = t.getSelectedRow();
            if (r < 0) {
                warn(this, "Please select an exam from the list first.");
                return;
            }
            Exam e = avail.get(r);
            if (!ask(this, "Start '" + e.title + "' now?\nYou have " + e.durationMin + " minute(s)."))
                return;
            startExam(student, e);
        }
    }

    static class MyResultsPage extends Page {
        final Student student;
        final DefaultTableModel m = model("Exam", "Score", "Percent", "Result", "Date");
        final JTable t = table(m);
        final JLabel summary = new JLabel(" ");

        MyResultsPage(Student s) {
            super("My Results", "Your previous exam attempts.");
            this.student = s;
            widths(t, 260, 100, 90, 80, 150);
            JPanel c = card();
            summary.setFont(font(Font.BOLD, 14));
            summary.setForeground(NAVY);
            c.add(scroll(t), BorderLayout.CENTER);
            c.add(summary, BorderLayout.SOUTH);
            add(c, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            m.setRowCount(0);
            int n = 0, pass = 0;
            double best = 0;
            for (Result r : results) {
                if (!r.studentId.equalsIgnoreCase(student.id)) continue;
                n++;
                if (r.passed) pass++;
                best = Math.max(best, r.percent());
                m.addRow(new Object[]{examTitle(r.examId), r.score + " / " + r.total,
                        String.format("%.1f%%", r.percent()), r.passed ? "PASS" : "FAIL", r.dateTime});
            }
            summary.setText(n == 0 ? "You have not taken any exam yet."
                    : "Attempts: " + n + "     Passed: " + pass + "     Best score: " + String.format("%.1f%%", best));
        }
    }

    static class StudentPasswordPage extends Page {
        StudentPasswordPage(Student s) {
            super("Change Password", "Keep your account secure.");
            PasswordCard pc = new PasswordCard(() -> s.password, np -> {
                s.password = np;
                saveStudents();
            });
            pc.setMaximumSize(new Dimension(560, 330));
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setOpaque(false);
            pc.setPreferredSize(new Dimension(520, 300));
            wrap.add(pc);
            add(wrap, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
        }
    }

    // =====================================================================
    //                          EXAM SESSION
    // =====================================================================
    static void startExam(Student s, Exam exam) {
        if (examPanel != null) root.remove(examPanel);
        examPanel = new ExamPanel(s, exam);
        root.add(examPanel, "EXAM");
        rootLayout.show(root, "EXAM");
        examActive = true;
        examPanel.start();
    }

    static void endExam() {
        examActive = false;
        if (examPanel != null) {
            root.remove(examPanel);
            examPanel = null;
        }
        rootLayout.show(root, "STUDENT");
        if (studentPanel != null) studentPanel.nav.click(1);
    }

    static class ExamPanel extends JPanel {
        final Student student;
        final Exam exam;
        final List<Question> qs;
        final int[] given;
        final long endTime;
        int idx = 0;
        boolean finished = false;

        final JLabel timerLbl = new JLabel("00:00", SwingConstants.CENTER);
        final JLabel progressLbl = new JLabel();
        final JLabel marksLbl = new JLabel();
        final JTextArea qText = new JTextArea(4, 40);
        final JRadioButton[] opts = new JRadioButton[4];
        final ButtonGroup group = new ButtonGroup();
        final List<StyledButton> palette = new ArrayList<>();
        StyledButton prevBtn, nextBtn;
        javax.swing.Timer timer;

        ExamPanel(Student s, Exam e) {
            this.student = s;
            this.exam = e;
            this.qs = new ArrayList<>(questionsOf(e.id));
            Collections.shuffle(qs);
            this.given = new int[qs.size()];
            this.endTime = System.currentTimeMillis() + e.durationMin * 60_000L;
            setLayout(new BorderLayout());
            setBackground(BG);
            add(buildHeader(), BorderLayout.NORTH);
            add(buildBody(), BorderLayout.CENTER);
            timer = new javax.swing.Timer(500, ev -> tick());
            show(0);
        }

        private JPanel buildHeader() {
            JPanel h = new JPanel(new BorderLayout());
            h.setBackground(NAVY);
            h.setBorder(pad(12, 20, 12, 20));
            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setOpaque(false);
            JLabel title = new JLabel(exam.title);
            title.setFont(font(Font.BOLD, 20));
            title.setForeground(WHITE);
            progressLbl.setFont(font(Font.PLAIN, 14));
            progressLbl.setForeground(new Color(0xCFE3F5));
            left.add(title);
            left.add(progressLbl);
            timerLbl.setOpaque(true);
            timerLbl.setBackground(new Color(0x16304F));
            timerLbl.setForeground(new Color(0x9EE6DC));
            timerLbl.setFont(font(Font.BOLD, 26));
            timerLbl.setBorder(pad(6, 18, 6, 18));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            JLabel tl = new JLabel("Time left:");
            tl.setForeground(new Color(0xCFE3F5));
            right.add(tl);
            right.add(timerLbl);
            h.add(left, BorderLayout.WEST);
            h.add(right, BorderLayout.EAST);
            return h;
        }

        private JPanel buildBody() {
            JPanel body = new JPanel(new BorderLayout(16, 0));
            body.setOpaque(false);
            body.setBorder(pad(20, 20, 20, 20));

            // ----- question card -----
            JPanel qCard = new JPanel(new BorderLayout(0, 12));
            qCard.setBackground(WHITE);
            qCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(18, 20, 18, 20)));

            marksLbl.setFont(font(Font.BOLD, 13));
            marksLbl.setForeground(TEAL);

            qText.setEditable(false);
            qText.setLineWrap(true);
            qText.setWrapStyleWord(true);
            qText.setFont(font(Font.BOLD, 19));
            qText.setForeground(NAVY);
            qText.setBackground(WHITE);
            JScrollPane qsp = new JScrollPane(qText);
            qsp.setBorder(null);
            qsp.setPreferredSize(new Dimension(100, 110));

            JPanel optPanel = new JPanel(new GridLayout(4, 1, 0, 10));
            optPanel.setBackground(WHITE);
            for (int k = 0; k < 4; k++) {
                final int kk = k;
                JRadioButton rb = new JRadioButton();
                rb.setFont(font(Font.PLAIN, 16));
                rb.setForeground(TEXT);
                rb.setOpaque(true);
                rb.setFocusPainted(false);
                rb.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(8, 12, 8, 12)));
                rb.addActionListener(ev -> {
                    given[idx] = kk + 1;
                    updateOptionColors();
                    refreshPalette();
                });
                group.add(rb);
                opts[k] = rb;
                optPanel.add(rb);
            }

            JPanel centerPane = new JPanel(new BorderLayout(0, 10));
            centerPane.setOpaque(false);
            centerPane.add(qsp, BorderLayout.NORTH);
            centerPane.add(optPanel, BorderLayout.CENTER);

            prevBtn = new StyledButton("\u25C0  Previous", BLUE);
            nextBtn = new StyledButton("Next  \u25B6", BLUE);
            StyledButton clear = new StyledButton("Clear Answer", GREY);
            StyledButton submit = new StyledButton("Submit Exam", GREEN);
            prevBtn.addActionListener(ev -> show(idx - 1));
            nextBtn.addActionListener(ev -> show(idx + 1));
            clear.addActionListener(ev -> {
                group.clearSelection();
                given[idx] = 0;
                updateOptionColors();
                refreshPalette();
            });
            submit.addActionListener(ev -> submit());
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            JPanel fl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            fl.setOpaque(false);
            fl.add(prevBtn);
            fl.add(nextBtn);
            fl.add(clear);
            JPanel fr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            fr.setOpaque(false);
            fr.add(submit);
            foot.add(fl, BorderLayout.WEST);
            foot.add(fr, BorderLayout.EAST);

            qCard.add(marksLbl, BorderLayout.NORTH);
            qCard.add(centerPane, BorderLayout.CENTER);
            qCard.add(foot, BorderLayout.SOUTH);

            // ----- palette -----
            JPanel pal = new JPanel(new BorderLayout(0, 10));
            pal.setBackground(WHITE);
            pal.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(14, 14, 14, 14)));
            pal.setPreferredSize(new Dimension(250, 0));
            JLabel pt = new JLabel("Question Palette");
            pt.setFont(font(Font.BOLD, 16));
            pt.setForeground(NAVY);
            JPanel grid = new JPanel(new GridLayout(0, 5, 6, 6));
            grid.setBackground(WHITE);
            for (int i = 0; i < qs.size(); i++) {
                final int ii = i;
                StyledButton b = new StyledButton(String.valueOf(i + 1), GREY);
                b.setBorder(pad(4, 4, 4, 4));
                b.setPreferredSize(new Dimension(38, 34));
                b.addActionListener(ev -> show(ii));
                palette.add(b);
                grid.add(b);
            }
            JPanel gridWrap = new JPanel(new BorderLayout());
            gridWrap.setBackground(WHITE);
            gridWrap.add(grid, BorderLayout.NORTH);
            JLabel legend = new JLabel("<html><span style='color:#1F3A5F'>&#9632; Current</span><br>"
                    + "<span style='color:#2A9D8F'>&#9632; Answered</span><br>"
                    + "<span style='color:#8D99AE'>&#9632; Not answered</span></html>");
            legend.setFont(font(Font.PLAIN, 13));
            pal.add(pt, BorderLayout.NORTH);
            pal.add(scroll(gridWrap), BorderLayout.CENTER);
            pal.add(legend, BorderLayout.SOUTH);

            body.add(qCard, BorderLayout.CENTER);
            body.add(pal, BorderLayout.EAST);
            return body;
        }

        void start() {
            timer.start();
            tick();
        }

        void tick() {
            if (finished) return;
            long left = endTime - System.currentTimeMillis();
            if (left <= 0) {
                timer.stop();
                timerLbl.setText("00:00");
                info(this, "Time is up! Your exam will be submitted now.");
                finish();
                return;
            }
            long sec = (left + 999) / 1000;
            timerLbl.setText(String.format("%02d:%02d", sec / 60, sec % 60));
            if (sec <= 30) timerLbl.setForeground(new Color(0xFF6B6B));
            else if (sec <= 60) timerLbl.setForeground(new Color(0xFFD166));
            else timerLbl.setForeground(new Color(0x9EE6DC));
        }

        void show(int i) {
            if (i < 0 || i >= qs.size()) return;
            idx = i;
            Question q = qs.get(i);
            progressLbl.setText("Question " + (i + 1) + " of " + qs.size());
            marksLbl.setText("Question " + (i + 1) + "   \u2022   " + q.marks + " mark(s)");
            qText.setText(q.text);
            qText.setCaretPosition(0);
            for (int k = 0; k < 4; k++) opts[k].setText((char) ('A' + k) + ")   " + q.options[k]);
            if (given[i] == 0) group.clearSelection();
            else opts[given[i] - 1].setSelected(true);
            updateOptionColors();
            prevBtn.setEnabled(i > 0);
            nextBtn.setEnabled(i < qs.size() - 1);
            refreshPalette();
        }

        void updateOptionColors() {
            for (int k = 0; k < 4; k++) {
                boolean sel = given[idx] == k + 1;
                opts[k].setBackground(sel ? new Color(0xDDF3F0) : new Color(0xF6F8FC));
                opts[k].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(sel ? TEAL : LINE, sel ? 2 : 1), pad(7, 11, 7, 11)));
            }
        }

        void refreshPalette() {
            for (int i = 0; i < palette.size(); i++) {
                palette.get(i).setBase(i == idx ? NAVY : (given[i] != 0 ? TEAL : GREY));
            }
        }

        void submit() {
            int un = 0;
            for (int g : given) if (g == 0) un++;
            String msg = un == 0 ? "Submit your exam now?"
                    : "You have " + un + " unanswered question(s).\nSubmit your exam anyway?";
            if (ask(this, msg)) finish();
        }

        void finish() {
            if (finished) return;
            finished = true;
            timer.stop();
            int score = 0, total = 0, correctCount = 0, attempted = 0;
            for (int i = 0; i < qs.size(); i++) {
                Question q = qs.get(i);
                total += q.marks;
                if (given[i] != 0) {
                    attempted++;
                    if (given[i] == q.correct) {
                        score += q.marks;
                        correctCount++;
                    }
                }
            }
            double percent = total == 0 ? 0 : (score * 100.0) / total;
            boolean passed = percent >= exam.passPercent;
            results.add(new Result(student.id, exam.id, score, total, LocalDateTime.now().format(DT), passed));
            saveResults();
            showResultDialog(score, total, attempted, correctCount, percent, passed);
            endExam();
        }

        void showResultDialog(int score, int total, int attempted, int correctCount, double percent,
                              boolean passed) {
            JDialog d = new JDialog(frame, "Exam Result", true);
            JPanel p = new JPanel(new BorderLayout(0, 14));
            p.setBackground(WHITE);
            p.setBorder(pad(18, 18, 18, 18));

            JPanel banner = new JPanel(new GridLayout(2, 1));
            banner.setBackground(passed ? GREEN : RED);
            banner.setBorder(pad(14, 10, 14, 10));
            JLabel st = new JLabel(passed ? "CONGRATULATIONS - YOU PASSED" : "YOU DID NOT PASS", SwingConstants.CENTER);
            st.setFont(font(Font.BOLD, 22));
            st.setForeground(WHITE);
            JLabel pc = new JLabel(String.format("%.1f%%   (%d / %d marks)", percent, score, total),
                    SwingConstants.CENTER);
            pc.setFont(font(Font.BOLD, 28));
            pc.setForeground(WHITE);
            banner.add(st);
            banner.add(pc);

            JPanel det = new JPanel(new GridLayout(0, 2, 10, 4));
            det.setBackground(WHITE);
            String[][] rows = {{"Exam", exam.title}, {"Questions attempted", attempted + " of " + qs.size()},
                    {"Correct answers", String.valueOf(correctCount)}, {"Pass mark", exam.passPercent + "%"}};
            for (String[] r : rows) {
                JLabel a = label(r[0]);
                a.setForeground(GREY);
                JLabel b = label(r[1]);
                b.setFont(font(Font.BOLD, 14));
                det.add(a);
                det.add(b);
            }

            JPanel mid = new JPanel(new BorderLayout(0, 10));
            mid.setBackground(WHITE);
            mid.add(det, BorderLayout.NORTH);

            DefaultTableModel rm = model("Question", "Your Answer", "Correct Answer");
            for (int i = 0; i < qs.size(); i++) {
                Question q = qs.get(i);
                if (given[i] != q.correct) {
                    rm.addRow(new Object[]{q.text, given[i] == 0 ? "(skipped)" : q.options[given[i] - 1],
                            q.options[q.correct - 1]});
                }
            }
            if (rm.getRowCount() > 0) {
                JLabel rt = new JLabel("Review - questions you missed");
                rt.setFont(font(Font.BOLD, 15));
                rt.setForeground(NAVY);
                JTable rtab = table(rm);
                widths(rtab, 360, 160, 160);
                JScrollPane sp = scroll(rtab);
                sp.setPreferredSize(new Dimension(720, 190));
                JPanel rp = new JPanel(new BorderLayout(0, 6));
                rp.setBackground(WHITE);
                rp.add(rt, BorderLayout.NORTH);
                rp.add(sp, BorderLayout.CENTER);
                mid.add(rp, BorderLayout.CENTER);
            } else {
                JLabel perfect = new JLabel("Perfect score - you answered every question correctly!");
                perfect.setFont(font(Font.BOLD, 15));
                perfect.setForeground(GREEN);
                mid.add(perfect, BorderLayout.CENTER);
            }

            StyledButton close = new StyledButton("Close", TEAL);
            close.addActionListener(ev -> d.dispose());
            JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bp.setBackground(WHITE);
            bp.add(close);

            p.add(banner, BorderLayout.NORTH);
            p.add(mid, BorderLayout.CENTER);
            p.add(bp, BorderLayout.SOUTH);
            d.setContentPane(p);
            d.pack();
            d.setLocationRelativeTo(frame);
            d.setVisible(true);
        }
    }
}

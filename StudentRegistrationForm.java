import java.awt.*;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import javax.swing.*;

public class StudentRegistrationForm extends JFrame {
    // Form components
    JTextField firstName, lastName, dob;
    JRadioButton male, female;
    JTextArea address;
    JButton submit, export;
    JTable table;
    ButtonGroup genderGroup;

    // Labels to translate
    JLabel lblFirstName, lblLastName, lblDob, lblGender, lblAddress;
    JComboBox<String> langSelector;

    public StudentRegistrationForm() {
        Font unicodeFont = new Font("Nirmala UI", Font.PLAIN, 14);
        UIManager.put("Label.font", unicodeFont);
        UIManager.put("Button.font", unicodeFont);
        UIManager.put("TextField.font", unicodeFont);
        UIManager.put("TextArea.font", unicodeFont);
        UIManager.put("ComboBox.font", unicodeFont);
        UIManager.put("Table.font", unicodeFont);

        setTitle("Student Registration Form");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        String[] languages = {"English", "Hindi", "Kannada"};
        langSelector = new JComboBox<>(languages);
        langSelector.addActionListener(e -> updateLanguage((String) langSelector.getSelectedItem()));
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        langPanel.add(new JLabel("Language:"));
        langPanel.add(langSelector);
        add(langPanel, BorderLayout.SOUTH);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.setBackground(new Color(255, 245, 235));

        firstName = new JTextField();
        lastName = new JTextField();
        dob = new JTextField();
        address = new JTextArea(3, 20);
        JScrollPane addrScroll = new JScrollPane(address);
        male = new JRadioButton("Male");
        female = new JRadioButton("Female");

        genderGroup = new ButtonGroup();
        genderGroup.add(male);
        genderGroup.add(female);

        lblFirstName = new JLabel("First Name:");
        lblLastName = new JLabel("Last Name:");
        lblDob = new JLabel("Date of Birth (dd/mm/yyyy):");
        lblGender = new JLabel("Gender:");
        lblAddress = new JLabel("Address:");

        formPanel.add(lblFirstName); formPanel.add(firstName);
        formPanel.add(lblLastName); formPanel.add(lastName);
        formPanel.add(lblDob); formPanel.add(dob);
        formPanel.add(lblGender);
        JPanel genderPanel = new JPanel();
        genderPanel.add(male); genderPanel.add(female);
        formPanel.add(genderPanel);
        formPanel.add(lblAddress); formPanel.add(addrScroll);

        submit = new JButton("Submit");
        export = new JButton("Export to Word");

        formPanel.add(submit);
        formPanel.add(export);
        add(formPanel, BorderLayout.NORTH);

        table = new JTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        submit.addActionListener(e -> insertStudent());
        export.addActionListener(e -> exportToWord());

        loadTable();
        setVisible(true);
    }

    private String translateText(String text, String targetLang) {
        try {
            String urlStr = "https://libretranslate.de/translate";
            String data = "q=" + URLEncoder.encode(text, "UTF-8") +
                          "&source=en&target=" + targetLang + "&format=text";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            conn.getOutputStream().write(data.getBytes());
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output, response = "";
            while ((output = in.readLine()) != null) {
                response += output;
            }
            in.close();
            return response.split("\"translatedText\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return text;
        }
    }

    private void insertStudent() {
        String fname = firstName.getText().trim();
        String lname = lastName.getText().trim();
        String dobText = dob.getText().trim();
        String gender = male.isSelected() ? "Male" : female.isSelected() ? "Female" : "";
        String addr = address.getText().trim();

        if (fname.isEmpty() || lname.isEmpty() || dobText.isEmpty() || gender.isEmpty() || addr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:students.db")) {
            Class.forName("org.sqlite.JDBC");
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS student_form (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT, last_name TEXT, dob TEXT, gender TEXT, address TEXT)");
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO student_form (first_name, last_name, dob, gender, address) VALUES (?, ?, ?, ?, ?)");
            pstmt.setString(1, fname);
            pstmt.setString(2, lname);
            pstmt.setString(3, dobText);
            pstmt.setString(4, gender);
            pstmt.setString(5, addr);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student added successfully!");
            clearForm();
            loadTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void loadTable() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:students.db")) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM student_form");
            int cols = rs.getMetaData().getColumnCount();
            String[] columnNames = new String[cols];
            for (int i = 0; i < cols; i++) columnNames[i] = rs.getMetaData().getColumnName(i + 1);
            rs.last(); int rows = rs.getRow(); rs.beforeFirst();
            String[][] data = new String[rows][cols];
            int row = 0;
            while (rs.next()) {
                for (int col = 0; col < cols; col++) data[row][col] = rs.getString(col + 1);
                row++;
            }
            table.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading table: " + e.getMessage());
        }
    }

    private void exportToWord() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:students.db")) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM student_form");
            java.util.List<String[]> records = new java.util.ArrayList<>();
            while (rs.next()) {
                records.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("dob"),
                    rs.getString("gender"),
                    rs.getString("address")
                });
            }
            writeDoc("StudentRecords_English.doc", records, new String[]{"ID", "Name", "DOB", "Gender", "Address"}, "en");
            writeDoc("StudentRecords_Hindi.doc", records, new String[]{"आईडी", "नाम", "जन्म तिथि", "लिंग", "पता"}, "hi");
            writeDoc("StudentRecords_Kannada.doc", records, new String[]{"ಐಡಿ", "ಹೆಸರು", "ಹುಟ್ಟಿದ ದಿನಾಂಕ", "ಲಿಂಗ", "ವಿಳಾಸ"}, "kn");
            JOptionPane.showMessageDialog(this, "Exported to 3 Word documents successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export Error: " + e.getMessage());
        }
    }

    private void writeDoc(String filename, java.util.List<String[]> records, String[] headers, String lang) throws Exception {
        FileWriter writer = new FileWriter(filename);
        writer.write("Student Records:\n\n");
        for (String[] rec : records) {
            writer.write(headers[0] + ": " + rec[0] + "\n");
            writer.write(headers[1] + ": " + translateText(rec[1], lang) + "\n");
            writer.write(headers[2] + ": " + rec[2] + "\n");
            writer.write(headers[3] + ": " + translateText(rec[3], lang) + "\n");
            writer.write(headers[4] + ": " + translateText(rec[4], lang) + "\n\n");
        }
        writer.close();
    }

    private void clearForm() {
        firstName.setText("");
        lastName.setText("");
        dob.setText("");
        genderGroup.clearSelection();
        address.setText("");
    }

    private void updateLanguage(String language) {
        switch (language) {
            case "Hindi":
                lblFirstName.setText("पहला नाम:");
                lblLastName.setText("अंतिम नाम:");
                lblDob.setText("जन्म तिथि (dd/mm/yyyy):");
                lblGender.setText("लिंग:");
                lblAddress.setText("पता:");
                submit.setText("जमा करें");
                export.setText("वर्ड में निर्यात करें");
                break;
            case "Kannada":
                lblFirstName.setText("ಮೊದಲ ಹೆಸರು:");
                lblLastName.setText("ಕೊನೆಯ ಹೆಸರು:");
                lblDob.setText("ಹುಟ್ತಿದ ದಿನಾಂಕ (dd/mm/yyyy):");
                lblGender.setText("ಲಿಂಗ:");
                lblAddress.setText("ವಿಳಾಸ:");
                submit.setText("ಸಲ್ಲಿಸು");
                export.setText("ವರ್ಡ್‌ಗೆ ಎಕ್ಸ್ಪೋರ್ಟ್ ಮಾಡಿ");
                break;
            default:
                lblFirstName.setText("First Name:");
                lblLastName.setText("Last Name:");
                lblDob.setText("Date of Birth (dd/mm/yyyy):");
                lblGender.setText("Gender:");
                lblAddress.setText("Address:");
                submit.setText("Submit");
                export.setText("Export to Word");
        }
    }

    public static void main(String[] args) {
        new StudentRegistrationForm();
    }
}

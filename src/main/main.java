import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.Scanner;

public class main {

    static config db = new config();
    static String role, status, dbPassword, fullName;
    static int userId;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== Barangay Document Request System ====");
            System.out.println("1. Log In");
            System.out.println("2. Online Registration");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    handleLogin(sc);
                    break;

                case 2:
                    handleRegistration(sc);
                    break;

                case 3:
                    System.out.println("Exiting system...");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid option, try again.");
            }
        }
    }

    private static void handleLogin(Scanner sc) {
    boolean loggedIn = false;
    config db = new config();  // IMPORTANT

    while (!loggedIn) {
        System.out.print("Enter Username (u_name): ");
        String username = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        try (Connection conn = connectDB()) {
            PreparedStatement state = conn.prepareStatement(
                "SELECT u_id, u_status, u_role, u_password, u_fullname FROM tbl_user WHERE u_name = ?"
            );
            state.setString(1, username);

            try (ResultSet rs = state.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("❌ Username not found!");
                    continue; // restart login loop
                }

                // Username exists → load details
                dbPassword = rs.getString("u_password");
                role = rs.getString("u_role");
                status = rs.getString("u_status");
                fullName = rs.getString("u_fullname");
                userId = rs.getInt("u_id");
            }

            // Now compare password
            if (!db.verifyPassword(password, dbPassword)) {
                System.out.println("❌ Incorrect password!");
                continue;
            }

            // Password is correct → check status
            if (status.equalsIgnoreCase("Pending")) {
                System.out.println("⏳ Your account is still pending approval.");
                loggedIn = true;

            } else if (status.equalsIgnoreCase("Disabled")) {
                System.out.println("❌ Your account has been disabled.");
                loggedIn = true;

            } else {
                System.out.println("✅ Login successful! Welcome, " + fullName);

                if (role.equalsIgnoreCase("Official") || role.equalsIgnoreCase("Admin")) {
                    new Officials().showAdminMenu(sc);
                } else {
                    new Residents().showResidentMenu(sc, userId);
                }
                loggedIn = true;
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }


                System.out.println();
            }
        }



    private static void handleRegistration(Scanner sc) {
        String newUser, newPass, newContact, newFullName, newRole;

        // Check username uniqueness
        while (true) {
            System.out.print("Enter Username (u_name): ");
            newUser = sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?")) {

                checkStmt.setString(1, newUser);
                ResultSet rs = checkStmt.executeQuery();

                boolean exists = rs.next() && rs.getInt(1) > 0;
                rs.close();

                if (exists) {
                    System.out.println("❌ Username already exists! Please try another one.\n");
                    continue;
                }
                break;

            } catch (SQLException e) {
                System.out.println("❌ Error checking username: " + e.getMessage());
                return;
            }
        }

        // Validate Full Name (must not exist already)
while (true) {
    System.out.print("Enter Full Name: ");
    newFullName = sc.nextLine().trim();

    if (newFullName.isEmpty()) {
        System.out.println("❌ Full name cannot be empty.\n");
        continue;
    }

    try (Connection conn = connectDB();
         PreparedStatement checkFullName = conn.prepareStatement(
                 "SELECT COUNT(*) FROM tbl_user WHERE LOWER(TRIM(u_fullname)) = LOWER(TRIM(?))"
         )) {

        checkFullName.setString(1, newFullName);
        ResultSet rsFull = checkFullName.executeQuery();

        boolean exists = rsFull.next() && rsFull.getInt(1) > 0;
        rsFull.close();

        if (exists) {
            System.out.println("❌ Full Name already exists! Please enter a different full name.\n");
            continue; // LOOP BACK
        }

        break; // VALID → EXIT LOOP

    } catch (SQLException e) {
        System.out.println("❌ Error checking full name: " + e.getMessage());
        continue;
    }
}


        // Enter Password
        System.out.print("Enter Password: ");
        newPass = sc.nextLine();
        String hashedPassword = db.hashPassword(newPass);

        // Validate Contact Number
        while (true) {
            System.out.print("Enter Contact Number: ");
            newContact = sc.nextLine();

            if (!newContact.matches("\\d{11}")) {
                System.out.println("❌ Invalid contact number! It must be exactly 11 digits.\n");
                continue;
            }

            try (Connection conn = connectDB();
                 PreparedStatement checkContact = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?")) {

                checkContact.setString(1, newContact);
                ResultSet rsContact = checkContact.executeQuery();

                boolean contactExists = rsContact.next() && rsContact.getInt(1) > 0;
                rsContact.close();

                if (contactExists) {
                    System.out.println("❌ Contact number already exists! Please try another.\n");
                    continue;
                }
                break;

            } catch (SQLException e) {
                System.out.println("❌ Error checking contact: " + e.getMessage());
                continue;
            }
        }

        // Ask for role (Official or Resident)
        while (true) {
            System.out.print("What user type are you? (Official or Resident): ");
            newRole = sc.nextLine().trim();

            if (newRole.equalsIgnoreCase("Official") || newRole.equalsIgnoreCase("Resident")) {
                newRole = Character.toUpperCase(newRole.charAt(0)) + newRole.substring(1).toLowerCase();
                break;
            } else {
                System.out.println("❌ Only choose 'Official' or 'Resident'.\n");
            }
        }

        // Insert new record - DO NOT RELY ON DEFAULT, EXPLICITLY SET u_status TO "Pending"
        String insertSQL = "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_contact, u_role, u_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectDB();
             PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

            insertStmt.setString(1, newUser);
            insertStmt.setString(2, newFullName);
            insertStmt.setString(3, hashedPassword);
            insertStmt.setString(4, newContact);
            insertStmt.setString(5, newRole);         // u_role = "Official" or "Resident"
            insertStmt.setString(6, "Pending");        // u_status = "Pending" ← 100% GUARANTEED

            insertStmt.executeUpdate();

            System.out.println("✅ Registration successful! Please wait for approval.");

        } catch (SQLException e) {
            System.out.println("❌ Error during registration: " + e.getMessage());
        }
    }
}
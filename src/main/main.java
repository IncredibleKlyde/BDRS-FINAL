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
                    if (rs.next()) {
                        dbPassword = rs.getString("u_password");
                        role = rs.getString("u_role");
                        status = rs.getString("u_status");
                        fullName = rs.getString("u_fullname");
                        userId = rs.getInt("u_id");
                    }
                    rs.close();

                    if (password.equals(dbPassword)) {
                        if (status.equalsIgnoreCase("Approved")) {
                            System.out.println("Login successful! Welcome, " + fullName);

                            if (role.equalsIgnoreCase("Official") || role.equalsIgnoreCase("Admin")) {
                                Officials officials = new Officials();
                                officials.showAdminMenu(sc);
                            } else {
                                Residents residents = new Residents();
                                residents.showResidentMenu(sc, userId);
                            }
                            loggedIn = true;
                        } else if (status.equalsIgnoreCase("Pending")) {
                            System.out.println("Your account is still pending approval.");
                            loggedIn = true;
                        } else {
                            System.out.println("Your account has been disabled.");
                            loggedIn = true;
                        }
                    } else {
                        System.out.println("Incorrect password! Please try again...");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }

            if (!loggedIn) {
                System.out.println();
            }
        }
    }

    private static void handleRegistration(Scanner sc) {
        String newUser;
        String newPass;
        String newContact;
        String newFullName;
        String newRole;

        // Check username uniqueness
        while (true) {
            System.out.print("Enter Username (u_name): ");
            newUser = sc.nextLine();

            try (Connection conn = connectDB()) {
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?"
                );
                checkStmt.setString(1, newUser);
                ResultSet rs = checkStmt.executeQuery();

                boolean exists = false;
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }

                rs.close();
                checkStmt.close();

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

        // Enter Full Name
        System.out.print("Enter Full Name: ");
        newFullName = sc.nextLine();

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

            try (Connection conn = connectDB()) {
                PreparedStatement checkContact = conn.prepareStatement(
                        "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?"
                );
                checkContact.setString(1, newContact);
                ResultSet rsContact = checkContact.executeQuery();

                boolean contactExists = false;
                if (rsContact.next() && rsContact.getInt(1) > 0) {
                    contactExists = true;
                }

                rsContact.close();
                checkContact.close();

                if (contactExists) {
                    System.out.println("❌ Contact number already exists! Please try another.\n");
                    continue;
                }
            } catch (SQLException e) {
                System.out.println("❌ Error checking contact: " + e.getMessage());
                continue;
            }

            break;
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

        // Insert new record safely
        try {
            db.updateRecord(
                    "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_status, u_contact, u_role) VALUES (?, ?, ?, 'Pending', ?, ?)",
                    newUser, newFullName, hashedPassword, newContact, newRole
            );

            System.out.println("✅ Registration successful! Please wait for approval.");
        } catch (Exception e) {
            System.out.println("❌ Error during registration: " + e.getMessage());
        }
    }
}
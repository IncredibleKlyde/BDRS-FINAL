import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Residents {

    config db = new config();

    public void showResidentMenu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n==== Resident Panel ====");
            System.out.println("1. Request Document");
            System.out.println("2. View My Request History and Info");
            System.out.println("3. Follow up/Cancel Request");
            System.out.println("4. Change Details and Password");
            System.out.println("5. Log Out");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    requestDocument(sc, userId);
                    break;
                case 2:
                    viewProfile(userId);
                    break;
                case 3:
                    followUpOrCancelRequest(sc, userId);
                    break;
                case 4:
                    changeDetailsAndPassword(sc, userId);
                    break;
                case 5:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("❌ Invalid option. Try again.");
            }
        }
    }

    private void requestDocument(Scanner sc, int userId) {
    System.out.println("\n==== Request Document ====");

    String sql = "SELECT d_id, d_doctype, d_fee FROM tbl_doc WHERE LOWER(TRIM(d_status)) = 'available'";

    List<Integer> docIds = new ArrayList<>();
    List<String> docTypes = new ArrayList<>();
    List<Integer> docFees = new ArrayList<>();

    // FIRST: Fetch available documents
    try (Connection conn = connectDB();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        int index = 1;
        while (rs.next()) {
            docIds.add(rs.getInt("d_id"));
            docTypes.add(rs.getString("d_doctype"));
            docFees.add(rs.getInt("d_fee"));
            System.out.println(index + ". " + rs.getString("d_doctype") + " (₱" + rs.getInt("d_fee") + ")");
            index++;
        }

        if (docIds.isEmpty()) {
            System.out.println("❌ No documents available to request.");
            return;
        }

        System.out.println(index + ". Go Back");

    } catch (SQLException e) {
        System.out.println("❌ Error fetching documents: " + e.getMessage());
        return;
    }

    // SECOND: User input
    System.out.print("Choose a document: ");
    int choice = sc.nextInt();
    sc.nextLine();

    if (choice == docIds.size() + 1) return;

    if (choice < 1 || choice > docIds.size()) {
        System.out.println("❌ Invalid option.");
        return;
    }

    String selectedType = docTypes.get(choice - 1);
    int selectedFee = docFees.get(choice - 1);
    int selectedDocId = docIds.get(choice - 1);

    // ⛔ NEW: BLOCK REQUEST IF DOCUMENT IS DISABLED (double-check)
    try (Connection conn = connectDB();
         PreparedStatement checkStmt = conn.prepareStatement(
                 "SELECT d_status FROM tbl_doc WHERE d_id = ?")) {

        checkStmt.setInt(1, selectedDocId);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            String status = rs.getString("d_status").trim().toLowerCase();

            if (!status.equals("available")) {
                System.out.println("❌ This document is DISABLED and cannot be requested.");
                System.out.println("🔁 Returning to Request Document menu...\n");
                return;
            }
        }
    } catch (SQLException e) {
        System.out.println("❌ Error checking document status: " + e.getMessage());
        return;
    }

    // Continue request
    System.out.println("\nYou selected: " + selectedType + " (₱" + selectedFee + ")");
    System.out.print("Enter purpose: ");
    String purpose = sc.nextLine().trim();

    System.out.print("Confirm submit request? (yes/no): ");
    String confirm = sc.nextLine().trim();

    if (!confirm.equalsIgnoreCase("yes")) {
        System.out.println("❌ Request cancelled.");
        return;
    }

    // THIRD: Insert request
    String insertSql = "INSERT INTO tbl_req (u_id, d_id, d_doctype, r_fee, r_purpose, r_status, r_date) "
            + "VALUES (?, ?, ?, ?, ?, 'Pending', datetime('now'))";

    try (Connection conn = connectDB();
         PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

        insertStmt.setInt(1, userId);
        insertStmt.setInt(2, selectedDocId);
        insertStmt.setString(3, selectedType);
        insertStmt.setInt(4, selectedFee);
        insertStmt.setString(5, purpose);
        insertStmt.executeUpdate();

        System.out.println("✅ Document request submitted successfully!");

    } catch (SQLException e) {
        System.out.println("❌ Error during insert: " + e.getMessage());
    }
}


    private void viewProfile(int userId) {
        String sql = "SELECT u_id, u_name, u_fullname, u_role, u_contact, u_status FROM tbl_user WHERE u_id = " + userId;
        String[] headers = {"ID", "Username", "Full Name", "Role", "Contact", "Status"};
        String[] cols = {"u_id", "u_name", "u_fullname", "u_role", "u_contact", "u_status"};
        db.viewRecords(sql, headers, cols);
    }

    private void followUpOrCancelRequest(Scanner sc, int userId) {
        System.out.println("\n==== My Document Requests ====");

        String sql = "SELECT r_id, d_doctype, r_purpose, r_fee, r_status, r_date FROM tbl_req WHERE u_id = ?";
        
        List<Integer> requestIds = new ArrayList<>();
        
        // FIRST: Fetch all requests and close connection
        try (Connection conn = connectDB();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            int index = 1;
            while (rs.next()) {
                int reqId = rs.getInt("r_id");
                requestIds.add(reqId);
                System.out.printf("%d. %s | Purpose: %s | Fee: ₱%d | Status: %s | Date: %s%n",
                        index,
                        rs.getString("d_doctype"),
                        rs.getString("r_purpose"),
                        rs.getInt("r_fee"),
                        rs.getString("r_status"),
                        rs.getString("r_date"));
                index++;
            }
            
            rs.close();

            if (requestIds.isEmpty()) {
                System.out.println("❌ You have no requests yet.");
                return;
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching requests: " + e.getMessage());
            return;
        }

        // SECOND: Get user input after connection is closed
        System.out.println((requestIds.size() + 1) + ". Go Back");
        System.out.print("Select a request to cancel or follow up: ");
        int choice = sc.nextInt();
        sc.nextLine();

        if (choice == requestIds.size() + 1) {
            return;
        }
        if (choice < 1 || choice > requestIds.size()) {
            System.out.println("❌ Invalid option.");
            return;
        }

        int selectedRequestId = requestIds.get(choice - 1);

        System.out.print("Do you want to cancel this request? (yes/no): ");
        String confirm = sc.nextLine().trim();

        if (confirm.equalsIgnoreCase("yes")) {
            // THIRD: Open new connection only for update
            String cancelSql = "UPDATE tbl_req SET r_status = 'Cancelled' WHERE r_id = ?";
            try (Connection conn = connectDB();
                 PreparedStatement cancelStmt = conn.prepareStatement(cancelSql)) {
                
                cancelStmt.setInt(1, selectedRequestId);
                cancelStmt.executeUpdate();
                System.out.println("✅ Request has been cancelled.");
                
            } catch (SQLException e) {
                System.out.println("❌ Error cancelling request: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️ Request not cancelled. You can follow up manually with admin if needed.");
        }
    }

    private void changeDetailsAndPassword(Scanner sc, int userId) {
        System.out.println("\n==== Change Details and Password ====");

        String currentName = null;
        String currentContact = null;
        String currentPassword = null;

        // FIRST: Fetch current details and close connection
        try (Connection conn = connectDB()) {
            String viewSql = "SELECT u_fullname, u_contact, u_password FROM tbl_user WHERE u_id = ?";
            PreparedStatement viewStmt = conn.prepareStatement(viewSql);
            viewStmt.setInt(1, userId);
            ResultSet rs = viewStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ User not found!");
                rs.close();
                viewStmt.close();
                return;
            }

            currentName = rs.getString("u_fullname");
            currentContact = rs.getString("u_contact");
            currentPassword = rs.getString("u_password");
            
            rs.close();
            viewStmt.close();

        } catch (SQLException e) {
            System.out.println("❌ Error fetching user details: " + e.getMessage());
            return;
        }

        // SECOND: Display and get user input (no database connection)
        System.out.println("Current Full Name: " + currentName);
        System.out.println("Current Contact: " + currentContact);
        System.out.println("-----------------------------------");

        System.out.print("Enter new full name (leave blank to keep current): ");
        String newName = sc.nextLine().trim();
        if (newName.isEmpty()) {
            newName = currentName;
        }

        System.out.print("Enter new contact number (leave blank to keep current): ");
        String newContact = sc.nextLine().trim();
        if (newContact.isEmpty()) {
            newContact = currentContact;
        }

        System.out.print("Enter new password (leave blank to keep current): ");
        String newPassword = sc.nextLine().trim();
        if (newPassword.isEmpty()) {
            newPassword = currentPassword;
        }

        System.out.print("Save changes? (yes/no): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("❌ Changes cancelled.");
            return;
        }

        // THIRD: Open new connection only for update
        try (Connection conn = connectDB()) {
            String updateSql = "UPDATE tbl_user SET u_fullname = ?, u_contact = ?, u_password = ? WHERE u_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, newName);
            updateStmt.setString(2, newContact);
            updateStmt.setString(3, newPassword);
            updateStmt.setInt(4, userId);
            updateStmt.executeUpdate();
            updateStmt.close();

            System.out.println("✅ Details updated successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error updating details: " + e.getMessage());
        }
    }
}
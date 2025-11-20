import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.Scanner;

public class Officials {

    config db = new config();

    public void showAdminMenu(Scanner sc) {
        while (true) {
            System.out.println("\n==== Admin Menu ====");
            System.out.println("1. Register");
            System.out.println("2. Update Account");
            System.out.println("3. View Data");
            System.out.println("4. Delete Account");
            System.out.println("5. Approve Registrations for Account");
            System.out.println("6. Approve/Update Document Requests");
            System.out.println("7. Disable Account");
            System.out.println("8. Document Panel");
            System.out.println("9. View Request Logs");
            System.out.println("10. Log out");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    registerUser(sc);
                    break;
                case 2:
                    updateAccount(sc);
                    break;
                case 3:
                    viewData();
                    break;
                case 4:
                    deleteAccount(sc);
                    break;
                case 5:
                    approveRegistrations(sc);
                    break;
                case 6:
                    approveDocumentRequests(sc);
                    break;
                case 7:
                    manageAccountStatus(sc);
                    break;
                case 8:
                    documentPanel(sc);
                    break;
                case 9:
                    viewRequestLogs();
                    break;
                case 10:
                    System.out.println("👋 You have been logged out successfully!");
                    return;
                default:
                    System.out.println("❌ Invalid option. Try again.");
            }
        }
    }

    private void registerUser(Scanner sc) {
        String uname, ufullname, upass, contact, utype;

        // STEP 1: Validate Username (separate connection)
        while (true) {
            System.out.print("Enter Username (Login name): ");
            uname = sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkUser = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?")) {

                checkUser.setString(1, uname);
                ResultSet rsUser = checkUser.executeQuery();

                boolean exists = rsUser.next() && rsUser.getInt(1) > 0;
                rsUser.close();

                if (exists) {
                    System.out.println("❌ Username already exists! Please try another.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("❌ Error checking username: " + e.getMessage());
                return;
            }
        }

        // STEP 2: Validate Full Name (separate connection)
        while (true) {
            System.out.print("Enter Full Name: ");
            ufullname = sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkFull = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_fullname = ?")) {

                checkFull.setString(1, ufullname);
                ResultSet rsFull = checkFull.executeQuery();

                boolean fullExists = rsFull.next() && rsFull.getInt(1) > 0;
                rsFull.close();

                if (fullExists) {
                    System.out.println("❌ Full Name already exists! Please try another.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("❌ Error checking full name: " + e.getMessage());
                return;
            }
        }

        // STEP 3: Get Password and hash it
        System.out.print("Enter Password: ");
        upass = sc.nextLine();
        String hashedPassword = db.hashPassword(upass);  // Hash the password

        // STEP 4: Validate Contact Number (separate connection)
        while (true) {
            System.out.print("Enter Contact Number: ");
            contact = sc.nextLine();

            if (!contact.matches("\\d{11}")) {
                System.out.println("❌ Invalid contact number! It must contain exactly 11 digits.\n");
                continue;
            }

            try (Connection conn = connectDB();
                 PreparedStatement checkContact = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?")) {

                checkContact.setString(1, contact);
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
                return;
            }
        }

        // STEP 5: Get User Type (no database)
        while (true) {
            System.out.print("What user type are you? (Official or Resident): ");
            utype = sc.nextLine().trim();

            if (utype.equalsIgnoreCase("Official") || utype.equalsIgnoreCase("Resident")) {
                break;
            } else {
                System.out.println("❌ Only choose 'Official' or 'Resident'.\n");
            }
        }

        // STEP 6: Insert record with HASHED password and PENDING status
        try (Connection conn = connectDB();
             PreparedStatement addStmt = conn.prepareStatement(
                     "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_contact, u_role, u_status) VALUES (?, ?, ?, ?, ?, 'Pending')")) {

            addStmt.setString(1, uname);
            addStmt.setString(2, ufullname);
            addStmt.setString(3, hashedPassword);  // Store HASHED password
            addStmt.setString(4, contact);
            addStmt.setString(5, utype);
            addStmt.executeUpdate();

            System.out.println("✅ User registered successfully! Status: Pending - awaiting approval.");

        } catch (SQLException e) {
            System.out.println("❌ Error inserting user: " + e.getMessage());
        }
    }

    private void updateAccount(Scanner sc) {
        // Show existing records before asking for ID
        String sql = "SELECT u_id, u_name, u_fullname, u_password, u_contact, u_status FROM tbl_user";
        String[] userHeaders = {"ID", "Username", "Full Name", "Password", "Contact", "Status"};
        String[] userColumns = {"u_id", "u_name", "u_fullname", "u_password", "u_contact", "u_status"};
        db.viewRecords(sql, userHeaders, userColumns);

        // Ask for ID and validate it exists
        int uid;
        while (true) {
            System.out.print("Enter User ID to update: ");
            uid = sc.nextInt();
            sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_id = ?")) {
                
                checkStmt.setInt(1, uid);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();

                if (rs.getInt(1) == 0) {
                    System.out.println("❌ ID not found! Please enter a valid ID.\n");
                    continue;
                }
                break;

            } catch (SQLException e) {
                System.out.println("Error checking ID: " + e.getMessage());
                return;
            }
        }

        String newName, newFullName, newPassword, newContact;

        // Check for duplicate Username
        while (true) {
            System.out.print("Enter new Username (Login name): ");
            newName = sc.nextLine();
            
            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_name = ? AND u_id != ?")) {
                
                checkStmt.setString(1, newName);
                checkStmt.setInt(2, uid);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                
                if (rs.getInt(1) > 0) {
                    System.out.println("❌ Username already exists! Please try another one.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("Error checking username: " + e.getMessage());
                return;
            }
        }

        // Check for duplicate Full Name
        while (true) {
            System.out.print("Enter new Full Name: ");
            newFullName = sc.nextLine();
            
            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_fullname = ? AND u_id != ?")) {
                
                checkStmt.setString(1, newFullName);
                checkStmt.setInt(2, uid);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                
                if (rs.getInt(1) > 0) {
                    System.out.println("❌ Full Name already exists! Please try another one.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("Error checking full name: " + e.getMessage());
                return;
            }
        }

        // Get password (no database)
        System.out.print("Enter new password: ");
        newPassword = sc.nextLine();

        // Check for duplicate Contact Number
        while (true) {
            System.out.print("Enter new contact number: ");
            newContact = sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ? AND u_id != ?")) {
                
                checkStmt.setString(1, newContact);
                checkStmt.setInt(2, uid);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                
                if (rs.getInt(1) > 0) {
                    System.out.println("❌ Contact number already exists! Please try another one.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("Error checking contact number: " + e.getMessage());
                return;
            }
        }

        // Proceed with update if all checks pass
        String updateSql = "UPDATE tbl_user SET u_name = ?, u_fullname = ?, u_password = ?, u_contact = ? WHERE u_id = ?";
        db.updateRecord(updateSql, newName, newFullName, newPassword, newContact, uid);
        System.out.println("✅ User information updated successfully!");
    }

    private void viewData() {
        String[] headers = {"ID", "Username", "Full Name", "Role", "Contact", "Status"};
        String[] cols = {"u_id", "u_name", "u_fullname", "u_role", "u_contact", "u_status"};
        db.viewRecords("SELECT * FROM tbl_user", headers, cols);
    }

    private void deleteAccount(Scanner sc) {
        // Show existing records before deleting
        String deletesql = "SELECT u_id, u_name, u_fullname, u_contact, u_status FROM tbl_user";
        String[] header = {"ID", "Username", "Full Name", "Contact", "Status"};
        String[] columns = {"u_id", "u_name", "u_fullname", "u_contact", "u_status"};
        db.viewRecords(deletesql, header, columns);

        int delId;

        // Validation loop: keeps asking until valid ID is entered
        while (true) {
            System.out.print("Enter User ID to delete: ");
            delId = sc.nextInt();
            sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_id = ?")) {
                
                checkStmt.setInt(1, delId);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();

                if (rs.getInt(1) == 0) {
                    System.out.println("❌ ID not found! Please enter a valid ID.\n");
                    continue;
                }
                break;
                
            } catch (SQLException e) {
                System.out.println("Error checking ID: " + e.getMessage());
                return;
            }
        }

        // Proceed to delete
        String deleteSql = "DELETE FROM tbl_user WHERE u_id = ?";
        db.deleteRecord(deleteSql, delId);
        System.out.println("✅ User deleted successfully!");
    }

    private void approveRegistrations(Scanner sc) {
        String pendingSql = "SELECT u_id, u_name, u_password, u_status FROM tbl_user WHERE u_status = 'Pending'";
        String[] citizensHeaders = {"ID", "Username", "Password", "Status"};
        String[] citizensColumns = {"u_id", "u_name", "u_password", "u_status"};
        db.viewRecords(pendingSql, citizensHeaders, citizensColumns);

        int id;
        while (true) {
            System.out.print("Enter ID to approve/deny: ");
            id = sc.nextInt();
            sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_user WHERE u_id = ? AND u_status = 'Pending'")) {
                
                checkStmt.setInt(1, id);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();

                if (rs.getInt(1) == 0) {
                    System.out.println("❌ ID not found in pending list! Please enter a valid ID.\n");
                    continue;
                }
                break;

            } catch (SQLException e) {
                System.out.println("Error checking ID: " + e.getMessage());
                return;
            }
        }

        System.out.print("Approve or Deny? (A/D): ");
        String decision = sc.nextLine().trim();
        String update = "UPDATE tbl_user SET u_status = ? WHERE u_id = ?";
        String status = null;
        
        if (decision.equalsIgnoreCase("A")) {
            status = "Approved";
        } else if (decision.equalsIgnoreCase("D")) {
            status = "Denied";
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        
        db.updateRecord(update, status, id);
        System.out.println("✅ Registration " + status.toLowerCase() + " successfully!");
    }

    private void approveDocumentRequests(Scanner sc) {
        System.out.println("\n==== Pending Document Requests ====");
        String[] rHeaders = {"Request ID", "User ID", "Document Type", "Fee", "Purpose", "Date", "Status", "Approval By"};
        String[] rCols = {"r_id", "u_id", "d_doctype", "d_fee", "r_purpose", "r_date", "r_status", "r_approvalby"};

        String viewQuery
                = "SELECT r.r_id, r.u_id, d.d_doctype, d.d_fee, r.r_purpose, r.r_date, r.r_status, "
                + "COALESCE(r.r_approvalby, 'Pending') AS r_approvalby "
                + "FROM tbl_req AS r "
                + "JOIN tbl_doc AS d ON r.d_id = d.d_id "
                + "WHERE r.r_status = 'Pending'";

        db.viewRecords(viewQuery, rHeaders, rCols);

        int rid;

        while (true) {
            System.out.print("Enter Request ID to process: ");
            rid = sc.nextInt();
            sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM tbl_req WHERE r_id = ? AND r_status = 'Pending'")) {
                
                checkStmt.setInt(1, rid);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();

                if (rs.getInt(1) == 0) {
                    System.out.println("❌ Request ID not found or not pending! Please enter a valid ID.\n");
                    continue;
                }
                break;
            } catch (SQLException e) {
                System.out.println("Error checking Request ID: " + e.getMessage());
                return;
            }
        }

        System.out.print("Approve or Deny? (A/D): ");
        String decisionDoc = sc.nextLine().trim();

        String statusDoc;
        if (decisionDoc.equalsIgnoreCase("A")) {
            statusDoc = "Approved";
        } else if (decisionDoc.equalsIgnoreCase("D")) {
            statusDoc = "Denied";
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        System.out.print("Enter your name (official approving/denying): ");
        String approvalBy = sc.nextLine();

        String updateDoc = "UPDATE tbl_req SET r_status = ?, r_approvalby = ? WHERE r_id = ?";
        db.updateRecord(updateDoc, statusDoc, approvalBy, rid);

        System.out.println("✅ Document request " + statusDoc.toLowerCase() + " successfully!");
    }

    private void manageAccountStatus(Scanner sc) {
        System.out.println("\n==== Manage Account Status ====");

        // Show users with status Approved or Disabled
        String sql = "SELECT u_id, u_name, u_fullname, u_contact, u_status FROM tbl_user WHERE u_status IN ('Approved','Disabled')";
        String[] headers = {"ID", "Username", "Full Name", "Contact", "Status"};
        String[] cols = {"u_id", "u_name", "u_fullname", "u_contact", "u_status"};
        db.viewRecords(sql, headers, cols);

        int userId;
        String currentStatus = null;

        // Get user ID and status
        while (true) {
            System.out.print("Enter User ID to modify: ");
            userId = sc.nextInt();
            sc.nextLine();

            try (Connection conn = connectDB();
                 PreparedStatement checkStmt = conn.prepareStatement(
                         "SELECT u_status FROM tbl_user WHERE u_id = ?")) {
                
                checkStmt.setInt(1, userId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("❌ User not found! Try again.\n");
                    continue;
                }

                currentStatus = rs.getString("u_status");
                break;

            } catch (SQLException e) {
                System.out.println("Error checking user: " + e.getMessage());
                return;
            }
        }

        // Display status and get action
        System.out.println("Current Status: " + currentStatus);
        System.out.print("Do you want to (D)isable or (E)nable this account? ");
        String action = sc.nextLine().trim();

        String newStatus = null;
        if (action.equalsIgnoreCase("D") && currentStatus.equals("Approved")) {
            newStatus = "Disabled";
        } else if (action.equalsIgnoreCase("E") && currentStatus.equals("Disabled")) {
            newStatus = "Approved";
        } else {
            System.out.println("⚠ Invalid action or status already set.");
            return;
        }

        // Update status
        String updateSQL = "UPDATE tbl_user SET u_status = ? WHERE u_id = ?";
        db.updateRecord(updateSQL, newStatus, userId);
        
        if (newStatus.equals("Disabled")) {
            System.out.println("✅ Account disabled successfully!");
        } else {
            System.out.println("🟢 Account enabled successfully!");
        }
    }

    private void documentPanel(Scanner sc) {
        int docChoice;
        do {
            System.out.println("\n==== Document Panel ====");
            System.out.println("1. Add document");
            System.out.println("2. Update document");
            System.out.println("3. View documents available to request");
            System.out.println("4. Disable/Enable document");
            System.out.println("5. Go back to Admin Panel");
            System.out.print("Enter choice: ");
            docChoice = sc.nextInt();
            sc.nextLine();

            switch (docChoice) {
                case 1:
                    addDocument(sc);
                    break;
                case 2:
                    updateDocument(sc);
                    break;
                case 3:
                    viewDocuments();
                    break;
                case 4:
                    toggleDocument(sc);
                    break;
                case 5:
                    System.out.println("📙 Returning to Admin Panel...");
                    break;
                default:
                    System.out.println("❌ Invalid choice, please try again.");
            }

        } while (docChoice != 5);
    }

    private void addDocument(Scanner sc) {
        System.out.print("Write the document name/type: ");
        String docType = sc.nextLine();

        System.out.print("Fee for that type of document: ");
        int fee = sc.nextInt();
        sc.nextLine();

        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO tbl_doc (d_doctype, d_fee, d_status) VALUES (?, ?, 'Available')")) {

            pstmt.setString(1, docType);
            pstmt.setInt(2, fee);
            pstmt.executeUpdate();
            System.out.println("✅ Document added successfully!");
            
        } catch (SQLException e) {
            System.out.println("❌ Error adding document: " + e.getMessage());
        }
    }

    private void updateDocument(Scanner sc) {
        System.out.println("\n==== List of Documents ====");
        
        int updateId;
        boolean hasDocs = false;
        
        // STEP 1: Display documents and close connection
        try (Connection conn = connectDB();
             PreparedStatement viewStmt = conn.prepareStatement("SELECT d_id, d_doctype, d_fee, d_status FROM tbl_doc");
             ResultSet rsView = viewStmt.executeQuery()) {

            while (rsView.next()) {
                hasDocs = true;
                System.out.printf("[%d] %-25s ₱%-5d  (%s)\n",
                        rsView.getInt("d_id"),
                        rsView.getString("d_doctype"),
                        rsView.getInt("d_fee"),
                        rsView.getString("d_status"));
            }

            if (!hasDocs) {
                System.out.println("⚠️  No documents found to update.");
                return;
            }

        } catch (SQLException e) {
            System.out.println("❌ Error viewing documents: " + e.getMessage());
            return;
        }

        // STEP 2: Get document ID and validate
        System.out.print("\nEnter the document ID to update: ");
        updateId = sc.nextInt();
        sc.nextLine();

        try (Connection conn = connectDB();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM tbl_doc WHERE d_id = ?")) {
            
            checkStmt.setInt(1, updateId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ No document found with that ID.");
                return;
            }

        } catch (SQLException e) {
            System.out.println("❌ Error checking document: " + e.getMessage());
            return;
        }

        // STEP 3: Get new details (no database)
        System.out.print("Enter new document name/type: ");
        String newType = sc.nextLine();

        System.out.print("Enter new fee for that document: ");
        int newFee = sc.nextInt();
        sc.nextLine();

        // STEP 4: Update document
        try (Connection conn = connectDB();
             PreparedStatement updateStmt = conn.prepareStatement(
                     "UPDATE tbl_doc SET d_doctype = ?, d_fee = ? WHERE d_id = ?")) {
            
            updateStmt.setString(1, newType);
            updateStmt.setInt(2, newFee);
            updateStmt.setInt(3, updateId);
            updateStmt.executeUpdate();

            System.out.println("✅ Document updated successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error updating document: " + e.getMessage());
        }
    }

    private void viewDocuments() {
        try (Connection conn = connectDB();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tbl_doc");
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n📜 Available Documents:");
            System.out.println("ID | Type | Fee | Status");
            System.out.println("----------------------------------");

            while (rs.next()) {
                System.out.printf("%d | %s | %d | %s%n",
                        rs.getInt("d_id"),
                        rs.getString("d_doctype"),
                        rs.getInt("d_fee"),
                        rs.getString("d_status"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error viewing documents: " + e.getMessage());
        }
    }

    private void toggleDocument(Scanner sc) {
        System.out.println("\n📜 Document List:");
        System.out.println("ID | Type | Fee | Status");
        System.out.println("----------------------------------");

        int docId;
        String currentStatus = null;
        
        // STEP 1: Display documents
        try (Connection conn = connectDB();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tbl_doc");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                System.out.printf("%d | %s | %d | %s%n",
                        rs.getInt("d_id"),
                        rs.getString("d_doctype"),
                        rs.getInt("d_fee"),
                        rs.getString("d_status"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error viewing documents: " + e.getMessage());
            return;
        }

        // STEP 2: Get document ID
        System.out.print("\nEnter document ID to modify: ");
        docId = sc.nextInt();
        sc.nextLine();

        // STEP 3: Check current status
        try (Connection conn = connectDB();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT d_status FROM tbl_doc WHERE d_id = ?")) {
            
            checkStmt.setInt(1, docId);
            ResultSet checkRs = checkStmt.executeQuery();

            if (!checkRs.next()) {
                System.out.println("❌ Document ID not found!");
                return;
            }

            currentStatus = checkRs.getString("d_status");

        } catch (SQLException e) {
            System.out.println("❌ Error checking document: " + e.getMessage());
            return;
        }

        // STEP 4: Get user choice (no database)
        String choice;
        String newStatus;
        
        if (currentStatus.equalsIgnoreCase("Available")) {
            System.out.print("This document is currently AVAILABLE. Do you want to disable it? (Y/N): ");
            choice = sc.nextLine();

            if (choice.equalsIgnoreCase("Y")) {
                newStatus = "Disabled";
            } else {
                System.out.println("⚙️ Action cancelled.");
                return;
            }
        } else if (currentStatus.equalsIgnoreCase("Disabled")) {
            System.out.print("This document is currently DISABLED. Do you want to enable it? (Y/N): ");
            choice = sc.nextLine();

            if (choice.equalsIgnoreCase("Y")) {
                newStatus = "Available";
            } else {
                System.out.println("⚙️ Action cancelled.");
                return;
            }
        } else {
            System.out.println("❌ Unknown status – cannot modify.");
            return;
        }

        // STEP 5: Update status
        try (Connection conn = connectDB();
             PreparedStatement updateStmt = conn.prepareStatement(
                     "UPDATE tbl_doc SET d_status = ? WHERE d_id = ?")) {
            
            updateStmt.setString(1, newStatus);
            updateStmt.setInt(2, docId);
            updateStmt.executeUpdate();

            if (newStatus.equals("Disabled")) {
                System.out.println("✅ Document has been DISABLED.");
            } else {
                System.out.println("✅ Document has been ENABLED (set to Available).");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error modifying document: " + e.getMessage());
        }
    }

    private void viewRequestLogs() {
        System.out.println("\n==== All Document Request Logs ====");
        String[] logHeaders = {"Request ID", "User ID", "Document Type", "Fee", "Purpose", "Date", "Status", "Approved By"};
        String[] logCols = {"r_id", "u_id", "d_doctype", "r_fee", "r_purpose", "r_date", "r_status", "r_approvalby"};

        String logQuery = "SELECT r_id, u_id, d_doctype, r_fee, r_purpose, r_date, r_status, "
                + "COALESCE(r_approvalby, 'Pending') AS r_approvalby "
                + "FROM tbl_req";

        db.viewRecords(logQuery, logHeaders, logCols);
    }
}